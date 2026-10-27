package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoPaymentClient;
import com.proyecto.servicios.entity.gestopago.EstadoTransaccion;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.TransaccionPago;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.EstatusCuenta;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.gestopago.*;
import com.proyecto.servicios.repositorys.gestopago.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoServicioService;
import feign.FeignException;
import feign.RetryableException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementación de la arquitectura de pagos de servicios financieros conectados
 * a la API externa de GestoPago (POST /sistema/service/sendTx.do).
 *
 * <p>Esta clase implementa:
 * <ul>
 *   <li>Bloqueo pesimista para concurrencia segura en saldos bancarios ({@code SELECT FOR UPDATE}).</li>
 *   <li>Prevención de transacciones duplicadas mediante clave de idempotencia estricta.</li>
 *   <li>Manejo robusto de la incertidumbre por timeouts (HTTP 504 / RetryableException) evitando falsos negativos.</li>
 *   <li>Gestión segura de credenciales y Bearer Tokens de GestoPago.</li>
 *   <li>Traducción limpia de errores Feign sin exponer trazas técnicas al cliente.</li>
 * </ul>
 */
@Service
@Slf4j
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(GestoPagoTxResponse.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Error al inicializar JAXBContext para GestoPagoTxResponse", e);
        }
    }

    private final CuentaRepository cuentaRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final GestoPagoProductService gestoPagoProductService;
    private final GestoPagoPaymentClient gestoPagoPaymentClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.bearer-token:}")
    private String fallbackBearerToken;

    public PagoServicioServiceImpl(CuentaRepository cuentaRepository,
                                  TransaccionPagoRepository transaccionPagoRepository,
                                  GestoPagoProductService gestoPagoProductService,
                                  GestoPagoPaymentClient gestoPagoPaymentClient,
                                  GestoPagoTokenService gestoPagoTokenService) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.gestoPagoProductService = gestoPagoProductService;
        this.gestoPagoPaymentClient = gestoPagoPaymentClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Override
    @Transactional
    public PagoServicioResponse pagarServicio(PagoServicioRequest request) {
        log.info("Iniciando procesamiento de pago de servicio. IdempotencyKey={}, Cuenta={}, Producto={}, Monto={}",
                request.getIdempotencyKey(), request.getNumeroCuenta(), request.getIdProducto(), request.getMonto());

        // 1. Control de Idempotencia: Verificar si la clave ya fue registrada
        Optional<TransaccionPago> txExistenteOpt = transaccionPagoRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (txExistenteOpt.isPresent()) {
            TransaccionPago txExistente = txExistenteOpt.get();
            if (txExistente.getEstatus() == EstadoTransaccion.EXITOSA) {
                log.info("Transacción ya completada exitosamente con anterioridad. Retornando respuesta idempotente para clave={}",
                        request.getIdempotencyKey());
                return mapToResponse(txExistente);
            }
            if (txExistente.getEstatus() == EstadoTransaccion.TIMEOUT_REVISION) {
                log.warn("Solicitud duplicada sobre transacción en revisión por timeout previo. IdempotencyKey={}",
                        request.getIdempotencyKey());
                throw new TransaccionDuplicadaException(request.getIdempotencyKey());
            }
            if (txExistente.getEstatus() == EstadoTransaccion.INICIADA) {
                log.warn("Solicitud duplicada: la transacción se encuentra actualmente en procesamiento. IdempotencyKey={}",
                        request.getIdempotencyKey());
                throw new TransaccionDuplicadaException(request.getIdempotencyKey());
            }
        }

        // 2. Concurrencia y Consistencia: Bloqueo Pesimista (PESSIMISTIC_WRITE) sobre la cuenta bancaria
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(request.getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(request.getNumeroCuenta()));

        // 3. Validaciones de Negocio Previas
        // A) Validar pertenencia de la cuenta al cliente
        if (cuenta.getCliente() == null || !cuenta.getCliente().getId().equals(request.getClienteId())) {
            log.error("Violación de seguridad: La cuenta '{}' no pertenece al cliente ID {}",
                    request.getNumeroCuenta(), request.getClienteId());
            throw new CuentaNoPerteneceAlClienteException(request.getNumeroCuenta(), request.getClienteId());
        }

        // B) Validar estatus del cliente
        if (!Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
            log.warn("El cliente ID {} se encuentra inactivo", request.getClienteId());
            throw new ClienteInactivoException("El cliente titular de la cuenta no se encuentra activo en el sistema");
        }

        // C) Validar estatus de la cuenta bancaria ('A' Activa)
        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            log.warn("La cuenta bancaria '{}' no está activa (Estatus: {})",
                    request.getNumeroCuenta(), cuenta.getEstatus());
            throw new CuentaInactivaException(request.getNumeroCuenta());
        }

        // D) Validar que el saldo de la cuenta sea estrictamente mayor o igual al monto a pagar
        if (cuenta.getSaldo().compareTo(request.getMonto()) < 0) {
            log.warn("Saldo insuficiente en cuenta '{}'. Saldo actual: {}, Monto requerido: {}",
                    request.getNumeroCuenta(), cuenta.getSaldo(), request.getMonto());
            throw new SaldoInsuficienteException(cuenta.getSaldo(), request.getMonto());
        }

        // 4. Validación de Reglas del Catálogo de GestoPago
        GestoPagoProductListResponse catalogo = gestoPagoProductService.obtenerProductos();
        GestoPagoProductoItem productoCatalogo = catalogo.getProductos().stream()
                .filter(p -> p.getIdProducto() != null && p.getIdProducto().equals(request.getIdProducto()))
                .findFirst()
                .orElseThrow(() -> new ProductoNoEncontradoException(request.getIdProducto()));

        // Validación de precio fijo si aplica
        validarReglasPrecio(productoCatalogo, request.getMonto());

        // Validación de dígito verificador si el producto lo requiere
        if (Boolean.TRUE.equals(productoCatalogo.getHasDigitoVerificador()) && StringUtils.isBlank(request.getDigitoVerificador())) {
            throw new ErrorValidacionException(String.format(
                    "El producto '%s' requiere obligatoriamente un dígito verificador", productoCatalogo.getProducto()));
        }

        // 5. Registro Inicial de la Transacción en estado INICIADA (Auditoría)
        BigDecimal saldoAnterior = cuenta.getSaldo();
        TransaccionPago transaccion = TransaccionPago.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .cliente(cuenta.getCliente())
                .cuenta(cuenta)
                .numeroCuenta(cuenta.getNumeroCuenta())
                .idProducto(request.getIdProducto())
                .servicio(productoCatalogo.getServicio())
                .producto(productoCatalogo.getProducto())
                .monto(request.getMonto())
                .saldoAnterior(saldoAnterior)
                .referenciaServicio(request.getReferencia())
                .digitoVerificador(request.getDigitoVerificador())
                .estatus(EstadoTransaccion.INICIADA)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        transaccion = transaccionPagoRepository.save(transaccion);

        // 6. Invocación al Servicio Externo de GestoPago (POST /sistema/service/sendTx.do)
        String token = resolverBearerToken();
        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        String responseXml;
        try {
            responseXml = invocarSendTx(
                    authorizationHeader,
                    request.getIdProducto(),
                    request.getMonto().toPlainString(),
                    request.getReferencia(),
                    request.getDigitoVerificador(),
                    productoCatalogo.getTipoFront(),
                    productoCatalogo.getIdServicio()
            );
        } catch (RetryableException e) {
            /*
             * MANEJO DE LA INCERTIDUMBRE POR TIMEOUTS (504 GATEWAY TIMEOUT):
             * Ante un timeout de lectura/conexión con el proveedor externo, se desconoce si la transacción
             * se aplicó o no en los sistemas centrales de GestoPago. Revertir o dar por fallida la operación
             * generaría el riesgo de falsos negativos (que el servicio se haya pagado pero el dinero no se debite).
             * Por tanto, la transacción se marca en estado TIMEOUT_REVISION para su posterior verificación
             * o conciliación, y se lanza una excepción amigable al cliente evitando el reintento automático no controlado.
             */
            log.error("Timeout de comunicación con GestoPago para idempotencyKey={}: {}",
                    request.getIdempotencyKey(), e.getMessage());
            transaccion.setEstatus(EstadoTransaccion.TIMEOUT_REVISION);
            transaccion.setMensajeRespuestaGestoPago("Tiempo de espera agotado con el proveedor externo");
            transaccionPagoRepository.save(transaccion);

            throw new ExternalIntegrationException(
                    "Tiempo de espera agotado con el proveedor GestoPago. La transacción [Clave: "
                            + request.getIdempotencyKey() + "] se encuentra en estado de verificación para confirmar su aplicación. "
                            + "Por favor no reintente el pago inmediatamente para evitar cobros duplicados.",
                    HttpStatus.GATEWAY_TIMEOUT, e);
        } catch (FeignException.Unauthorized e) {
            log.error("Error 401 Unauthorized en sendTx.do de GestoPago");
            transaccion.setEstatus(EstadoTransaccion.FALLIDA);
            transaccion.setMensajeRespuestaGestoPago("Autenticación no válida en el servicio externo");
            transaccionPagoRepository.save(transaccion);
            throw new ExternalIntegrationException("Autenticación no válida en el servicio externo GestoPago", HttpStatus.UNAUTHORIZED, e);
        } catch (FeignException.Forbidden e) {
            log.error("Error 403 Forbidden en sendTx.do de GestoPago: Token expirado o no autorizado");
            transaccion.setEstatus(EstadoTransaccion.FALLIDA);
            transaccion.setMensajeRespuestaGestoPago("Token expirado o no autorizado");
            transaccionPagoRepository.save(transaccion);
            throw new ExternalIntegrationException("El Bearer Token ha expirado o no cuenta con autorización", HttpStatus.FORBIDDEN, e);
        } catch (FeignException e) {
            log.error("Error Feign al consumir sendTx.do de GestoPago (Status {}): {}", e.status(), e.getMessage());
            transaccion.setEstatus(EstadoTransaccion.FALLIDA);
            transaccion.setMensajeRespuestaGestoPago("Error en la comunicación con el servicio externo");
            transaccionPagoRepository.save(transaccion);
            HttpStatus status = HttpStatus.resolve(e.status()) != null ? HttpStatus.resolve(e.status()) : HttpStatus.BAD_GATEWAY;
            throw new ExternalIntegrationException("Error de comunicación con el servicio externo GestoPago", status, e);
        } catch (ExternalIntegrationException e) {
            transaccion.setEstatus(EstadoTransaccion.FALLIDA);
            transaccion.setMensajeRespuestaGestoPago(e.getMessage());
            transaccionPagoRepository.save(transaccion);
            throw e;
        } catch (Exception e) {
            log.error("Fallo inesperado al procesar pago en GestoPago: {}", e.getMessage(), e);
            transaccion.setEstatus(EstadoTransaccion.FALLIDA);
            transaccion.setMensajeRespuestaGestoPago("Fallo inesperado en el procesamiento de la transacción");
            transaccionPagoRepository.save(transaccion);
            throw new ExternalIntegrationException("Fallo inesperado al conectar con el servicio externo GestoPago", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        // 7. Procesamiento del XML de Respuesta de GestoPago
        GestoPagoTxResponse txResponse = unmarshalXml(responseXml);

        if (txResponse.getMensaje() == null) {
            transaccion.setEstatus(EstadoTransaccion.FALLIDA);
            transaccionPagoRepository.save(transaccion);
            throw new ExternalIntegrationException("Respuesta sin encabezado de mensaje devuelta por GestoPago", HttpStatus.BAD_GATEWAY);
        }

        String codigoRespuesta = txResponse.getMensaje().getCodigo();
        String textoRespuesta = txResponse.getMensaje().getTexto();
        transaccion.setCodigoRespuestaGestoPago(codigoRespuesta);
        transaccion.setMensajeRespuestaGestoPago(textoRespuesta);

        // En GestoPago, el código "01" representa éxito
        if (!"01".equals(codigoRespuesta)) {
            log.warn("Transacción rechazada por GestoPago. Código: [{}], Mensaje: {}", codigoRespuesta, textoRespuesta);
            transaccion.setEstatus(EstadoTransaccion.FALLIDA);
            transaccionPagoRepository.save(transaccion);
            // Saldo permanece intacto
            throw new ExternalIntegrationException(
                    "GestoPago rechazó la transacción: " + textoRespuesta, HttpStatus.BAD_REQUEST);
        }

        // 8. Débito Efectivo de la Cuenta Bancaria y Finalización Exitosa
        BigDecimal nuevoSaldo = cuenta.getSaldo().subtract(request.getMonto());
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);

        transaccion.setEstatus(EstadoTransaccion.EXITOSA);
        transaccion.setSaldoPosterior(nuevoSaldo);

        if (txResponse.getTransaccion() != null) {
            GestoPagoTransaccionItem item = txResponse.getTransaccion();
            transaccion.setFolioGestoPago(item.getFolio());
            transaccion.setAutorizacionGestoPago(item.getAutorizacion());
            transaccion.setTicketGestoPago(item.getTicket());
        }

        transaccion = transaccionPagoRepository.save(transaccion);
        log.info("Pago procesado exitosamente. IdempotencyKey={}, FolioGestoPago={}, NuevoSaldo={}",
                request.getIdempotencyKey(), transaccion.getFolioGestoPago(), nuevoSaldo);

        return mapToResponse(transaccion);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoServicioResponse consultarTransaccion(PagoConsultaRequest request) {
        log.info("Consultando estado de transacción con idempotencyKey={}", request.getIdempotencyKey());
        TransaccionPago transaccion = transaccionPagoRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .orElseThrow(() -> new NegocioException(
                        "No se encontró ninguna transacción registrada con la clave de idempotencia: " + request.getIdempotencyKey(),
                        HttpStatus.NOT_FOUND));

        return mapToResponse(transaccion);
    }

    private void validarReglasPrecio(GestoPagoProductoItem producto, BigDecimal monto) {
        String precioConfigurado = producto.getPrecio();
        if (StringUtils.isNotBlank(precioConfigurado)) {
            try {
                BigDecimal precioFijo = new BigDecimal(precioConfigurado.trim());
                if (precioFijo.compareTo(BigDecimal.ZERO) > 0) {
                    if (precioFijo.compareTo(monto) != 0) {
                        log.warn("Monto solicitado ({}) no coincide con precio fijo del producto ({})",
                                monto, precioFijo);
                        throw new MontoInvalidoProductoException(String.format(
                                "El monto enviado ($%s) no coincide con el precio fijo configurado para el producto '%s' ($%s)",
                                monto.toPlainString(), producto.getProducto(), precioFijo.toPlainString()));
                    }
                }
            } catch (NumberFormatException ignored) {
                // Precios no numéricos (ej. "VARIABLE", "LIBRE") admiten cualquier monto válido mayor a 0
            }
        }
    }

    private String invocarSendTx(String authorizationHeader,
                                 Integer idProducto,
                                 String monto,
                                 String referencia,
                                 String digitoVerificador,
                                 Integer tipoFront,
                                 Integer idServicio) {
        log.info("Invocando cliente Feign POST /sistema/service/sendTx.do idProducto={} monto={} ref={}",
                idProducto, monto, referencia);
        long startTime = System.currentTimeMillis();
        try {
            return gestoPagoPaymentClient.sendTx(
                    authorizationHeader,
                    idProducto,
                    monto,
                    referencia,
                    digitoVerificador,
                    tipoFront,
                    idServicio
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Fin de la invocación externa a sendTx.do en {} ms", duration);
        }
    }

    private String resolverBearerToken() {
        Optional<GestoPagoToken> tokenOpt = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);

        if (tokenOpt.isPresent() && StringUtils.isNotBlank(tokenOpt.get().getToken())) {
            return tokenOpt.get().getToken();
        }

        if (StringUtils.isNotBlank(fallbackBearerToken)) {
            log.info("Utilizando token de respaldo configurado en application.properties para pago");
            return fallbackBearerToken;
        }

        log.info("No se encontró token activo en BD para pago. Intentando renovación de token...");
        try {
            gestoPagoTokenService.renovarToken();
            tokenOpt = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            if (tokenOpt.isPresent() && StringUtils.isNotBlank(tokenOpt.get().getToken())) {
                return tokenOpt.get().getToken();
            }
        } catch (Exception e) {
            log.error("Error al intentar renovar token de GestoPago: {}", e.getMessage());
        }

        throw new ExternalIntegrationException("No se dispone de un Bearer Token válido para procesar el pago", HttpStatus.UNAUTHORIZED);
    }

    private GestoPagoTxResponse unmarshalXml(String xmlContent) {
        if (StringUtils.isBlank(xmlContent)) {
            log.error("La respuesta XML obtenida de sendTx.do está vacía");
            throw new ExternalIntegrationException("Respuesta vacía del servicio externo GestoPago", HttpStatus.BAD_GATEWAY);
        }

        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new StringReader(xmlContent));
            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            GestoPagoTxResponse response = (GestoPagoTxResponse) unmarshaller.unmarshal(xmlStreamReader);

            if (response == null || response.getMensaje() == null) {
                log.error("Estructura XML de sendTx.do no coincide con el formato esperado");
                throw new ExternalIntegrationException("Formato XML de respuesta no válido devuelto por GestoPago", HttpStatus.BAD_GATEWAY);
            }

            return response;
        } catch (JAXBException | XMLStreamException e) {
            log.error("Error al deserializar el XML devuelto por GestoPago sendTx.do: {}", e.getMessage());
            throw new ExternalIntegrationException("Error en el procesamiento del XML devuelto por el servicio externo GestoPago", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private PagoServicioResponse mapToResponse(TransaccionPago tx) {
        return PagoServicioResponse.builder()
                .idTransaccion(tx.getId())
                .idempotencyKey(tx.getIdempotencyKey())
                .numeroCuenta(tx.getNumeroCuenta())
                .clienteId(tx.getCliente() != null ? tx.getCliente().getId() : null)
                .idProducto(tx.getIdProducto())
                .servicio(tx.getServicio())
                .producto(tx.getProducto())
                .monto(tx.getMonto())
                .saldoAnterior(tx.getSaldoAnterior())
                .saldoNuevo(tx.getSaldoPosterior())
                .referencia(tx.getReferenciaServicio())
                .estado(tx.getEstatus() != null ? tx.getEstatus().name() : null)
                .codigoGestoPago(tx.getCodigoRespuestaGestoPago())
                .mensajeGestoPago(tx.getMensajeRespuestaGestoPago())
                .folioGestoPago(tx.getFolioGestoPago())
                .autorizacionGestoPago(tx.getAutorizacionGestoPago())
                .ticket(tx.getTicketGestoPago())
                .fecha(tx.getFechaCreacion())
                .build();
    }
}
