package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ExternalIntegrationException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.Impl.GestoPagoProductServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductServiceImplTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @InjectMocks
    private GestoPagoProductServiceImpl gestoPagoProductService;

    private GestoPagoToken tokenDummy;
    private String xmlExitoso;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gestoPagoProductService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gestoPagoProductService, "codigoDispositivo", "GPS83-TPV-17");

        tokenDummy = new GestoPagoToken();
        tokenDummy.setId(1);
        tokenDummy.setIdDistribuidor(83);
        tokenDummy.setCodigoDispositivo("GPS83-TPV-17");
        tokenDummy.setToken("token_prueba_12345");
        tokenDummy.setActivo(true);

        xmlExitoso = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<RESPONSE>"
                + "    <MENSAJE>"
                + "        <CODIGO>01</CODIGO>"
                + "        <TEXTO>Operacion realizada con exito</TEXTO>"
                + "    </MENSAJE>"
                + "    <PRODUCTOS>"
                + "        <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun' idServicio='56' idProducto='185' idCatTipoServicio='15' tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c'>"
                + "            <legend><![CDATA[Leyenda de comprobante de pago AGUAKAN]]></legend>"
                + "        </producto>"
                + "    </PRODUCTOS>"
                + "</RESPONSE>";
    }

    @Test
    @DisplayName("Debe consultar la lista de productos exitosamente cuando existe token activo y XML válido")
    void obtenerProductos_Exitoso() {
        // Arrange
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"))).thenReturn(Optional.of(tokenDummy));
        when(gestoPagoProductClient.getProductList("Bearer token_prueba_12345")).thenReturn(xmlExitoso);

        // Act
        GestoPagoProductListResponse response = gestoPagoProductService.obtenerProductos();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMensaje());
        assertEquals("01", response.getMensaje().getCodigo());
        assertEquals("Operacion realizada con exito", response.getMensaje().getTexto());
        assertEquals(1, response.getProductos().size());
        assertEquals("AGUAKAN (Cancun)", response.getProductos().get(0).getServicio());
        assertEquals("Agua Cancun", response.getProductos().get(0).getProducto());
        assertEquals(56, response.getProductos().get(0).getIdServicio());
        assertEquals(185, response.getProductos().get(0).getIdProducto());
        assertEquals("Leyenda de comprobante de pago AGUAKAN", response.getProductos().get(0).getLegend());

        verify(gestoPagoTokenService, times(1)).obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"));
        verify(gestoPagoProductClient, times(1)).getProductList("Bearer token_prueba_12345");
    }

    @Test
    @DisplayName("Debe lanzar ExternalIntegrationException FORBIDDEN cuando el token ha expirado (HTTP 403)")
    void obtenerProductos_ErrorForbidden_TokenExpirado() {
        // Arrange
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"))).thenReturn(Optional.of(tokenDummy));

        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                new HashMap<>(), null, new RequestTemplate());
        FeignException.Forbidden forbiddenException = new FeignException.Forbidden("Token EXPIRED", request, null, new HashMap<>());

        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(forbiddenException);

        // Act & Assert
        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () -> {
            gestoPagoProductService.obtenerProductos();
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getMessage().contains("expirado"));
    }

    @Test
    @DisplayName("Debe lanzar ExternalIntegrationException UNAUTHORIZED cuando no hay token disponible")
    void obtenerProductos_SinTokenDisponible() {
        // Arrange
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"))).thenReturn(Optional.empty());

        // Act & Assert
        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () -> {
            gestoPagoProductService.obtenerProductos();
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getMessage().contains("Bearer Token"));
    }

    @Test
    @DisplayName("Debe lanzar ExternalIntegrationException BAD_GATEWAY si la respuesta XML está vacía")
    void obtenerProductos_RespuestaXmlVacia() {
        // Arrange
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"))).thenReturn(Optional.of(tokenDummy));
        when(gestoPagoProductClient.getProductList(anyString())).thenReturn("");

        // Act & Assert
        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () -> {
            gestoPagoProductService.obtenerProductos();
        });

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertTrue(exception.getMessage().contains("Respuesta vacía"));
    }

    @Test
    @DisplayName("Debe lanzar ExternalIntegrationException INTERNAL_SERVER_ERROR si el XML es malformado")
    void obtenerProductos_XmlMalFormado() {
        // Arrange
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"))).thenReturn(Optional.of(tokenDummy));
        when(gestoPagoProductClient.getProductList(anyString())).thenReturn("<RESPONSE><MENSAJE>XML corrupto sin cerrar");

        // Act & Assert
        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () -> {
            gestoPagoProductService.obtenerProductos();
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertTrue(exception.getMessage().contains("procesamiento del XML"));
    }

    @Test
    @DisplayName("Debe capturar FeignException genérica y relanzarla como ExternalIntegrationException")
    void obtenerProductos_FeignGenericException() {
        // Arrange
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"))).thenReturn(Optional.of(tokenDummy));

        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignException = new FeignException.BadGateway("502 Bad Gateway", request, null, new HashMap<>());

        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(feignException);

        // Act & Assert
        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () -> {
            gestoPagoProductService.obtenerProductos();
        });

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }
}
