package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ExternalIntegrationException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.bearer-token:}")
    private String fallbackBearerToken;

    public GestoPagoProductServiceImpl(GestoPagoProductClient gestoPagoProductClient,
                                        GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Override
    public GestoPagoProductListResponse obtenerProductos() {
        log.info("Iniciando consumo del servicio externo GET /sistema/service/getProductList.do");

        String token = resolverBearerToken();
        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        String responseXml;
        try {
            responseXml = gestoPagoProductClient.getProductList(authorizationHeader);
            log.info("Respuesta obtenida exitosamente del servicio externo GestoPago");
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

        return unmarshalXml(responseXml);
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
            JAXBContext context = JAXBContext.newInstance(GestoPagoProductListResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xmlContent);
            GestoPagoProductListResponse response = (GestoPagoProductListResponse) unmarshaller.unmarshal(reader);

            if (response == null || response.getMensaje() == null) {
                log.error("Estructura de respuesta XML no coincide con el formato esperado");
                throw new ExternalIntegrationException("Formato XML de respuesta no válido", HttpStatus.BAD_GATEWAY);
            }

            if (!"01".equals(response.getMensaje().getCodigo())) {
                log.warn("Servicio GestoPago retornó código de respuesta no exitoso: [{}] - {}",
                        response.getMensaje().getCodigo(), response.getMensaje().getTexto());
            }

            return response;
        } catch (JAXBException e) {
            log.error("Error al deserializar el XML devuelto por GestoPago: {}", e.getMessage());
            throw new ExternalIntegrationException("Error en el procesamiento del XML devuelto por el servicio externo", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
