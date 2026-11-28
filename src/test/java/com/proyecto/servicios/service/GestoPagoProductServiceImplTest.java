package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ExternalIntegrationException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.repositorys.gestopago.CatalogoProductoRepository;
import com.proyecto.servicios.service.Impl.GestoPagoProductServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductServiceImplTest {

    @Mock
    private CatalogoProductoRepository catalogoProductoRepository;

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @InjectMocks
    private GestoPagoProductServiceImpl gestoPagoProductService;

    private GestoPagoToken tokenDummy;
    private CatalogoProducto productoDummy;
    private String xmlExitoso;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gestoPagoProductService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gestoPagoProductService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(gestoPagoProductService, "fallbackBearerToken", "");

        tokenDummy = new GestoPagoToken();
        tokenDummy.setId(1);
        tokenDummy.setIdDistribuidor(83);
        tokenDummy.setCodigoDispositivo("GPS83-TPV-17");
        tokenDummy.setToken("token_prueba_12345");
        tokenDummy.setActivo(true);

        productoDummy = new CatalogoProducto();
        productoDummy.setId(1L);
        productoDummy.setIdProducto(185);
        productoDummy.setIdServicio(56);
        productoDummy.setServicio("AGUAKAN (Cancun)");
        productoDummy.setNombreProducto("Agua Cancun");
        productoDummy.setIdCatTipoServicio(15);
        productoDummy.setTipoFront(2);
        productoDummy.setHasDigitoVerificador(false);
        productoDummy.setPrecio("10.0");
        productoDummy.setShowAyuda(false);
        productoDummy.setTipoReferencia("c");
        productoDummy.setLegend("Leyenda de comprobante de pago AGUAKAN");
        productoDummy.setUrlLogo("/gestopago/logo/185");
        productoDummy.setFechaActualizacion(LocalDateTime.now());

        xmlExitoso = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<RESPONSE>"
                + "    <MENSAJE>"
                + "        <CODIGO>01</CODIGO>"
                + "        <TEXTO>Operacion realizada con exito</TEXTO>"
                + "    </MENSAJE>"
                + "    <PRODUCTOS>"
                + "        <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun' idServicio='56' idProducto='185' idCatTipoServicio='15' tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c' urlLogo='/gestopago/logo/185'>"
                + "            <legend><![CDATA[Leyenda de comprobante de pago AGUAKAN]]></legend>"
                + "        </producto>"
                + "    </PRODUCTOS>"
                + "</RESPONSE>";
    }

    // ─── obtenerProductos: lee desde la BD ───────────────────────────────────

    @Test
    @DisplayName("obtenerProductos debe retornar productos desde la base de datos con urlLogo completa")
    void obtenerProductos_Exitoso_DesdeDB() {
        // Arrange
        when(catalogoProductoRepository.findAllByOrderByServicioAscNombreProductoAsc())
                .thenReturn(List.of(productoDummy));

        // Act
        GestoPagoProductListResponse response = gestoPagoProductService.obtenerProductos();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMensaje());
        assertEquals("01", response.getMensaje().getCodigo());
        assertEquals(1, response.getProductos().size());

        var item = response.getProductos().get(0);
        assertEquals("AGUAKAN (Cancun)", item.getServicio());
        assertEquals("Agua Cancun", item.getProducto());
        assertEquals(185, item.getIdProducto());
        assertEquals(56, item.getIdServicio());
        assertEquals("Leyenda de comprobante de pago AGUAKAN", item.getLegend());
        assertEquals("https://gestopago.portalventas.net/gestopago/logo/185", item.getUrlLogo());

        // No debe llamar al cliente externo
        verifyNoInteractions(gestoPagoProductClient);
        verifyNoInteractions(gestoPagoTokenService);
    }

    @Test
    @DisplayName("obtenerProductos debe retornar lista vacía cuando no hay productos en BD")
    void obtenerProductos_SinProductos_ListaVacia() {
        when(catalogoProductoRepository.findAllByOrderByServicioAscNombreProductoAsc())
                .thenReturn(List.of());

        GestoPagoProductListResponse response = gestoPagoProductService.obtenerProductos();

        assertNotNull(response);
        assertEquals("01", response.getMensaje().getCodigo());
        assertTrue(response.getProductos().isEmpty());
        verifyNoInteractions(gestoPagoProductClient);
    }

    // ─── sincronizarCatalogoDesdeGestoPago: llama al API ────────────────────

    @Test
    @DisplayName("sincronizarCatalogo debe actualizar la BD con productos del XML de GestoPago")
    void sincronizarCatalogo_Exitoso() {
        // Sin registro reciente → no hay cooldown
        when(catalogoProductoRepository.findTopByFechaActualizacionIsNotNullOrderByFechaActualizacionDesc())
                .thenReturn(Optional.empty());
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17")))
                .thenReturn(Optional.of(tokenDummy));
        when(gestoPagoProductClient.getProductList("Bearer token_prueba_12345"))
                .thenReturn(xmlExitoso);
        when(catalogoProductoRepository.findByIdProducto(185))
                .thenReturn(Optional.empty());
        when(catalogoProductoRepository.findAllByOrderByServicioAscNombreProductoAsc())
                .thenReturn(List.of(productoDummy));

        GestoPagoProductListResponse response = gestoPagoProductService.sincronizarCatalogoDesdeGestoPago();

        assertNotNull(response);
        assertEquals("01", response.getMensaje().getCodigo());
        verify(gestoPagoProductClient, times(1)).getProductList("Bearer token_prueba_12345");
        verify(catalogoProductoRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    @DisplayName("sincronizarCatalogo debe respetar el cooldown de 24h y no llamar al API")
    void sincronizarCatalogo_Cooldown24h_NoLlamaAPI() {
        CatalogoProducto reciente = new CatalogoProducto();
        reciente.setFechaActualizacion(LocalDateTime.now().minusHours(2)); // 2h atrás, dentro del cooldown

        when(catalogoProductoRepository.findTopByFechaActualizacionIsNotNullOrderByFechaActualizacionDesc())
                .thenReturn(Optional.of(reciente));
        when(catalogoProductoRepository.findAllByOrderByServicioAscNombreProductoAsc())
                .thenReturn(List.of(productoDummy));

        GestoPagoProductListResponse response = gestoPagoProductService.sincronizarCatalogoDesdeGestoPago();

        assertNotNull(response);
        // No debe llamar al cliente externo porque está en cooldown
        verifyNoInteractions(gestoPagoProductClient);
        verifyNoInteractions(gestoPagoTokenService);
    }

    @Test
    @DisplayName("sincronizarCatalogo debe lanzar FORBIDDEN cuando el token ha expirado (HTTP 403)")
    void sincronizarCatalogo_ErrorForbidden_TokenExpirado() {
        when(catalogoProductoRepository.findTopByFechaActualizacionIsNotNullOrderByFechaActualizacionDesc())
                .thenReturn(Optional.empty());
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17")))
                .thenReturn(Optional.of(tokenDummy));

        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                new HashMap<>(), null, new RequestTemplate());
        FeignException.Forbidden forbiddenException = new FeignException.Forbidden("Forbidden", request, null, new HashMap<>());
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(forbiddenException);

        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () ->
                gestoPagoProductService.sincronizarCatalogoDesdeGestoPago());

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    @DisplayName("sincronizarCatalogo debe lanzar GATEWAY_TIMEOUT cuando hay timeout (RetryableException)")
    void sincronizarCatalogo_ErrorTimeout_RetryableException() {
        when(catalogoProductoRepository.findTopByFechaActualizacionIsNotNullOrderByFechaActualizacionDesc())
                .thenReturn(Optional.empty());
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17")))
                .thenReturn(Optional.of(tokenDummy));

        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                new HashMap<>(), null, new RequestTemplate());
        RetryableException retryableException = new RetryableException(
                504, "Read timed out", Request.HttpMethod.GET, new Date(), request);
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(retryableException);

        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () ->
                gestoPagoProductService.sincronizarCatalogoDesdeGestoPago());

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatus());
        assertTrue(exception.getMessage().contains("Tiempo de espera"));
    }

    @Test
    @DisplayName("sincronizarCatalogo debe lanzar UNAUTHORIZED cuando el cliente retorna 401")
    void sincronizarCatalogo_ErrorUnauthorized() {
        when(catalogoProductoRepository.findTopByFechaActualizacionIsNotNullOrderByFechaActualizacionDesc())
                .thenReturn(Optional.empty());
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17")))
                .thenReturn(Optional.of(tokenDummy));

        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                new HashMap<>(), null, new RequestTemplate());
        FeignException.Unauthorized unauthorizedException = new FeignException.Unauthorized("Unauthorized", request, null, new HashMap<>());
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(unauthorizedException);

        ExternalIntegrationException exception = assertThrows(ExternalIntegrationException.class, () ->
                gestoPagoProductService.sincronizarCatalogoDesdeGestoPago());

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getMessage().contains("Autenticación no válida"));
    }

    // ─── normalizarUrlLogo ────────────────────────────────────────────────────

    @Test
    @DisplayName("normalizarUrlLogo debe anteponer la URL base cuando el path empieza con /")
    void normalizarUrlLogo_PathRelativo_AnteponeBase() {
        String resultado = GestoPagoProductServiceImpl.normalizarUrlLogo("/gestopago/logo/185");
        assertEquals("https://gestopago.portalventas.net/gestopago/logo/185", resultado);
    }

    @Test
    @DisplayName("normalizarUrlLogo debe retornar null cuando el path es null o vacío")
    void normalizarUrlLogo_PathNulo_RetornaNull() {
        assertNull(GestoPagoProductServiceImpl.normalizarUrlLogo(null));
        assertNull(GestoPagoProductServiceImpl.normalizarUrlLogo(""));
        assertNull(GestoPagoProductServiceImpl.normalizarUrlLogo("   "));
    }

    @Test
    @DisplayName("normalizarUrlLogo debe retornar la URL tal cual cuando ya es absoluta")
    void normalizarUrlLogo_PathAbsoluto_SinCambios() {
        String url = "https://gestopago.portalventas.net/gestopago/logo/185";
        assertEquals(url, GestoPagoProductServiceImpl.normalizarUrlLogo(url));
    }
}
