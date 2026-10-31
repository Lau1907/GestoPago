package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ExternalIntegrationException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(GestoPagoProductListResponse.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Error al inicializar JAXBContext para GestoPagoProductListResponse", e);
        }
    }

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.bearer-token:}")
    private String fallbackBearerToken;

    @Value("${gestopago.auth.password:}")
    private String password;

    @Value("${gestopago.mock.enabled:false}")
    private boolean mockEnabled;

    public GestoPagoProductServiceImpl(GestoPagoProductClient gestoPagoProductClient,
                                        GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Override
    public GestoPagoProductListResponse obtenerProductos() {
        log.info("Iniciando consumo del servicio externo GET /sistema/service/getProductList.do");
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

        return unmarshalXml(responseXml);
    }

    private String invocarServicioExterno(String authorizationHeader) {
        try {
            String responseXml = gestoPagoProductClient.getProductList(authorizationHeader);
            log.info("Respuesta obtenida exitosamente del servicio externo GestoPago");
            return responseXml;
        } catch (RetryableException e) {
            if (mockEnabled) {
                log.warn("Timeout al consumir servicio GestoPago. Modo Mock activo: interceptando y retornando catálogo simulado");
                return generarXmlCatalogoSimulado();
            }
            log.error("Timeout al consumir el servicio externo GestoPago: {}", e.getMessage());
            throw new ExternalIntegrationException("Tiempo de espera agotado con el servicio externo", HttpStatus.GATEWAY_TIMEOUT, e);
        } catch (FeignException.Forbidden e) {
            if (mockEnabled) {
                log.warn("Error 403 Forbidden en GestoPago. Modo Mock activo: interceptando y retornando catálogo simulado");
                return generarXmlCatalogoSimulado();
            }
            log.error("Error 403 Forbidden al consumir el servicio externo GestoPago: Token expirado o no autorizado");
            throw new ExternalIntegrationException("El Bearer Token ha expirado o no es válido", HttpStatus.FORBIDDEN, e);
        } catch (FeignException.Unauthorized e) {
            if (mockEnabled) {
                log.warn("Error 401 Unauthorized en GestoPago. Modo Mock activo: interceptando y retornando catálogo simulado");
                return generarXmlCatalogoSimulado();
            }
            log.error("Error 401 Unauthorized al consumir el servicio externo GestoPago");
            throw new ExternalIntegrationException("Autenticación no válida en el servicio externo", HttpStatus.UNAUTHORIZED, e);
        } catch (FeignException e) {
            if (mockEnabled) {
                log.warn("Error Feign ({}) en GestoPago. Modo Mock activo: interceptando y retornando catálogo simulado", e.status());
                return generarXmlCatalogoSimulado();
            }
            log.error("Error en comunicación con GestoPago (Status {}): {}", e.status(), e.getMessage());
            HttpStatus status = HttpStatus.resolve(e.status()) != null ? HttpStatus.resolve(e.status()) : HttpStatus.BAD_GATEWAY;
            throw new ExternalIntegrationException("Error de comunicación con el servicio externo GestoPago", status, e);
        } catch (Exception e) {
            if (mockEnabled) {
                log.warn("Error de conexión con GestoPago: {}. Modo Mock activo: retornando catálogo simulado", e.getMessage());
                return generarXmlCatalogoSimulado();
            }
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

        if (mockEnabled) {
            log.info("Modo Mock activo: utilizando token simulado para consulta de productos");
            return "mock-gestopago-jwt-token-distribuidor-" + idDistribuidor;
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

        if (mockEnabled) {
            return "mock-gestopago-jwt-token-distribuidor-" + idDistribuidor;
        }

        throw new ExternalIntegrationException("No se dispone de un Bearer Token válido para autenticación", HttpStatus.UNAUTHORIZED);
    }

    private String generarXmlCatalogoSimulado() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>Operacion realizada con exito (Catalogo Simulado)</TEXTO>
                    </MENSAJE>
                    <PRODUCTOS>
                        <producto servicio="CFE" producto="CFE SUMINISTRADOR DE SERVICIOS BASICOS" idServicio="1" idProducto="1" idCatTipoServicio="1" tipoFront="1" hasDigitoVerificador="false" precio="VARIABLE" showAyuda="false" tipoReferencia="Servicio">
                            <legend><![CDATA[Comprobante de pago CFE]]></legend>
                        </producto>
                        <producto servicio="AGUAKAN (Cancun)" producto="Agua Cancun" idServicio="56" idProducto="185" idCatTipoServicio="15" tipoFront="2" hasDigitoVerificador="false" precio="10.0" showAyuda="false" tipoReferencia="c">
                            <legend><![CDATA[Leyenda de comprobante de pago AGUAKAN]]></legend>
                        </producto>
                    </PRODUCTOS>
                </RESPONSE>
                """;
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
