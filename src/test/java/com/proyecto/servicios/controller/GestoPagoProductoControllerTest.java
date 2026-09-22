package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ExternalIntegrationException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.gestopago.GestoPagoMensajeResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoItem;
import com.proyecto.servicios.service.GestoPagoProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GestoPagoProductService gestoPagoProductService;

    @InjectMocks
    private GestoPagoProductoController gestoPagoProductoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gestoPagoProductoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /gestopago/productos debe retornar HTTP 200 OK y la lista de productos en formato JSON")
    void obtenerProductos_RetornaHttp200() throws Exception {
        // Arrange
        GestoPagoMensajeResponse mensaje = GestoPagoMensajeResponse.builder()
                .codigo("01")
                .texto("Operacion realizada con exito")
                .build();

        GestoPagoProductoItem productoItem = GestoPagoProductoItem.builder()
                .servicio("AGUAKAN")
                .producto("Agua Cancun")
                .idServicio(56)
                .idProducto(185)
                .precio("10.0")
                .build();

        GestoPagoProductListResponse mockResponse = GestoPagoProductListResponse.builder()
                .mensaje(mensaje)
                .productos(List.of(productoItem))
                .build();

        when(gestoPagoProductService.obtenerProductos()).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/gestopago/productos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje.codigo").value("01"))
                .andExpect(jsonPath("$.mensaje.texto").value("Operacion realizada con exito"))
                .andExpect(jsonPath("$.productos[0].producto").value("Agua Cancun"))
                .andExpect(jsonPath("$.productos[0].idServicio").value(56));
    }

    @Test
    @DisplayName("GET /gestopago/productos debe retornar HTTP 403 Forbidden cuando el servicio lanza ExternalIntegrationException (FORBIDDEN)")
    void obtenerProductos_RetornaHttp403_CuandoTokenExpirado() throws Exception {
        // Arrange
        when(gestoPagoProductService.obtenerProductos())
                .thenThrow(new ExternalIntegrationException("El Bearer Token ha expirado", HttpStatus.FORBIDDEN));

        // Act & Assert
        mockMvc.perform(get("/gestopago/productos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value(403))
                .andExpect(jsonPath("$.mensaje").value("El Bearer Token ha expirado"));
    }
}
