package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ExternalIntegrationException;
import com.proyecto.servicios.model.gestopago.GestoPagoMensajeResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoItem;
import com.proyecto.servicios.repositorys.gestopago.CatalogoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.GestoPagoTokenService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    public static final String GESTOPAGO_LOGO_BASE_URL = "https://gestopago.portalventas.net";
    private static final String RELATIVE_LOGO_PREFIX = "/gestopago/logo/";
    private static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(GestoPagoProductListResponse.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Error al inicializar JAXBContext para GestoPagoProductListResponse", e);
        }
    }

    private final CatalogoProductoRepository catalogoProductoRepository;
    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.bearer-token:}")
    private String fallbackBearerToken;

    public GestoPagoProductServiceImpl(CatalogoProductoRepository catalogoProductoRepository,
                                        GestoPagoProductClient gestoPagoProductClient,
                                        GestoPagoTokenService gestoPagoTokenService) {
        this.catalogoProductoRepository = catalogoProductoRepository;
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public GestoPagoProductListResponse obtenerProductos() {
        List<CatalogoProducto> entidades = catalogoProductoRepository.findAllByOrderByServicioAscNombreProductoAsc();
        List<GestoPagoProductoItem> items = entidades.stream()
                .map(this::mapToItem)
                .toList();

        return GestoPagoProductListResponse.builder()
                .mensaje(GestoPagoMensajeResponse.builder()
                        .codigo("01")
                        .texto("Catálogo obtenido desde base de datos")
                        .build())
                .productos(items)
                .build();
    }

    @Override
    @Transactional
    public GestoPagoProductListResponse sincronizarCatalogoDesdeGestoPago() {
        Optional<CatalogoProducto> ultimaSync = catalogoProductoRepository
                .findTopByFechaActualizacionIsNotNullOrderByFechaActualizacionDesc();

        if (ultimaSync.isPresent()
                && ultimaSync.get().getFechaActualizacion() != null
                && ultimaSync.get().getFechaActualizacion().isAfter(LocalDateTime.now().minusHours(24))) {
            log.info("Sincronización omitida: el catálogo ya fue actualizado desde GestoPago hace menos de 24 horas ({}).",
                    ultimaSync.get().getFechaActualizacion());
            return obtenerProductos();
        }

        log.info("Iniciando sincronización oficial GET /sistema/service/getProductList.do");
        long startTime = System.currentTimeMillis();
        String token = resolverBearerToken();
        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        String responseXml;
        try {
            responseXml = invocarServicioExterno(authorizationHeader);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Fin de la invocación al servicio externo GestoPago en {} ms", duration);
        }

        GestoPagoProductListResponse apiResponse = unmarshalXml(responseXml);
        if (apiResponse.getProductos() != null && !apiResponse.getProductos().isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now();
            List<CatalogoProducto> actualizados = new ArrayList<>(apiResponse.getProductos().size());

            for (GestoPagoProductoItem item : apiResponse.getProductos()) {
                if (item.getIdProducto() == null || item.getIdServicio() == null) {
                    continue;
                }
                CatalogoProducto entidad = catalogoProductoRepository.findByIdProducto(item.getIdProducto())
                        .orElseGet(CatalogoProducto::new);

                entidad.setIdProducto(item.getIdProducto());
                entidad.setIdServicio(item.getIdServicio());
                entidad.setServicio(StringUtils.defaultIfBlank(item.getServicio(), "Servicio"));
                entidad.setNombreProducto(StringUtils.defaultIfBlank(item.getProducto(), "Producto"));
                entidad.setIdCatTipoServicio(item.getIdCatTipoServicio());
                entidad.setTipoFront(item.getTipoFront());
                entidad.setHasDigitoVerificador(Boolean.TRUE.equals(item.getHasDigitoVerificador()));
                entidad.setPrecio(item.getPrecio());
                entidad.setShowAyuda(Boolean.TRUE.equals(item.getShowAyuda()));
                entidad.setTipoReferencia(item.getTipoReferencia());
                entidad.setLegend(item.getLegend());
                if (StringUtils.isNotBlank(item.getUrlLogo())) {
                    entidad.setUrlLogo(item.getUrlLogo().trim());
                } else if (StringUtils.isBlank(entidad.getUrlLogo())) {
                    entidad.setUrlLogo(RELATIVE_LOGO_PREFIX + item.getIdServicio());
                }
                entidad.setFechaActualizacion(ahora);
                actualizados.add(entidad);
            }

            catalogoProductoRepository.saveAll(actualizados);
            log.info("Catálogo sincronizado desde GestoPago: {} productos actualizados.", actualizados.size());
        }

        return obtenerProductos();
    }

    public static String normalizarUrlLogo(String urlLogo) {
        if (StringUtils.isBlank(urlLogo)) {
            return null;
        }
        String trimmed = urlLogo.trim();
        if (trimmed.startsWith(RELATIVE_LOGO_PREFIX)) {
            return GESTOPAGO_LOGO_BASE_URL + trimmed;
        }
        if (trimmed.startsWith("/")) {
            return GESTOPAGO_LOGO_BASE_URL + trimmed;
        }
        return trimmed;
    }

    private GestoPagoProductoItem mapToItem(CatalogoProducto entity) {
        return GestoPagoProductoItem.builder()
                .servicio(entity.getServicio())
                .producto(entity.getNombreProducto())
                .idServicio(entity.getIdServicio())
                .idProducto(entity.getIdProducto())
                .idCatTipoServicio(entity.getIdCatTipoServicio())
                .tipoFront(entity.getTipoFront())
                .hasDigitoVerificador(Boolean.TRUE.equals(entity.getHasDigitoVerificador()))
                .precio(entity.getPrecio())
                .showAyuda(Boolean.TRUE.equals(entity.getShowAyuda()))
                .tipoReferencia(entity.getTipoReferencia())
                .legend(entity.getLegend())
                .urlLogo(normalizarUrlLogo(entity.getUrlLogo()))
                .build();
    }

    private String invocarServicioExterno(String authorizationHeader) {
        try {
            String responseXml = gestoPagoProductClient.getProductList(authorizationHeader);
            log.info("Respuesta obtenida exitosamente del servicio externo GestoPago");
            return responseXml;
        } catch (RetryableException e) {
            log.error("Timeout al consumir el servicio externo GestoPago: {}", e.getMessage());
            throw new ExternalIntegrationException("Tiempo de espera agotado con el servicio externo", HttpStatus.GATEWAY_TIMEOUT, e);
        } catch (FeignException.Forbidden e) {
            log.error("Error 403 Forbidden al consumir el servicio externo GestoPago: Token expirado o no autorizado");
            throw new ExternalIntegrationException("El Bearer Token ha expirado o no es válido", HttpStatus.FORBIDDEN, e);
        } catch (FeignException.Unauthorized e) {
            log.error("Error 401 Unauthorized al consumir el servicio externo GestoPago");
            throw new ExternalIntegrationException("Autenticación no válida en el servicio externo", HttpStatus.UNAUTHORIZED, e);
        } catch (FeignException e) {
            log.error("Error en comunicación con GestoPago (Status {}): {}", e.status(), e.getMessage());
            HttpStatus status = HttpStatus.resolve(e.status()) != null ? HttpStatus.resolve(e.status()) : HttpStatus.BAD_GATEWAY;
            throw new ExternalIntegrationException("Error de comunicación con el servicio externo GestoPago", status, e);
        } catch (Exception e) {
            log.error("Error inesperado de red o cliente al invocar GestoPago: {}", e.getMessage());
            throw new ExternalIntegrationException("Fallo inesperado al conectar con el servicio externo", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private String resolverBearerToken() {
        Optional<GestoPagoToken> tokenOpt = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);

        if (tokenOpt.isPresent() && StringUtils.isNotBlank(tokenOpt.get().getToken())) {
            return tokenOpt.get().getToken();
        }

        if (StringUtils.isNotBlank(fallbackBearerToken)) {
            log.info("Utilizando token de respaldo configurado en application.properties");
            return fallbackBearerToken;
        }

        log.info("No se encontró token activo en BD ni propiedad. Intentando renovación de token...");
        try {
            gestoPagoTokenService.renovarToken();
            tokenOpt = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            if (tokenOpt.isPresent() && StringUtils.isNotBlank(tokenOpt.get().getToken())) {
                return tokenOpt.get().getToken();
            }
        } catch (Exception e) {
            log.error("Error al intentar renovar token de GestoPago: {}", e.getMessage());
        }

        throw new ExternalIntegrationException("No se dispone de un Bearer Token válido para autenticación", HttpStatus.UNAUTHORIZED);
    }

    private GestoPagoProductListResponse unmarshalXml(String xmlContent) {
        if (StringUtils.isBlank(xmlContent)) {
            log.error("La respuesta XML obtenida del servicio externo está vacía");
            throw new ExternalIntegrationException("Respuesta vacía del servicio externo", HttpStatus.BAD_GATEWAY);
        }

        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new StringReader(xmlContent));
            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            GestoPagoProductListResponse response = (GestoPagoProductListResponse) unmarshaller.unmarshal(xmlStreamReader);

            if (response == null || response.getMensaje() == null) {
                log.error("Estructura de respuesta XML no coincide con el formato esperado");
                throw new ExternalIntegrationException("Formato XML de respuesta no válido", HttpStatus.BAD_GATEWAY);
            }

            if (!"01".equals(response.getMensaje().getCodigo())) {
                log.warn("Servicio GestoPago retornó código de respuesta no exitoso: [{}] - {}",
                        response.getMensaje().getCodigo(), response.getMensaje().getTexto());
            }

            return response;
        } catch (JAXBException | XMLStreamException e) {
            log.error("Error al deserializar el XML devuelto por GestoPago: {}", e.getMessage());
            throw new ExternalIntegrationException("Error en el procesamiento del XML devuelto por el servicio externo", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
