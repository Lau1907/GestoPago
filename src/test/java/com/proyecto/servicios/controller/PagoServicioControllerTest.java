package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.gestopago.PagoConsultaRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.service.PagoServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagoServicioControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PagoServicioService pagoServicioService;

    @InjectMocks
    private PagoServicioController pagoServicioController;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(pagoServicioController)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /pagos/servicio debe procesar exitosamente y retornar HTTP 200 OK")
    void pagarServicio_Valido_Retorna200() throws Exception {
        PagoServicioResponse mockResponse = PagoServicioResponse.builder()
                .idTransaccion(101)
                .idempotencyKey("TX-20261027-001")
                .numeroCuenta("1234567890")
                .clienteId(1)
                .idProducto(185)
                .servicio("AGUAKAN (Cancun)")
                .producto("Agua Cancun")
                .monto(new BigDecimal("10.00"))
                .saldoAnterior(new BigDecimal("500.00"))
                .saldoNuevo(new BigDecimal("490.00"))
                .referencia("9981234567")
                .estado("EXITOSA")
                .codigoGestoPago("01")
                .folioGestoPago("GP-FOLIO-123")
                .build();

        when(pagoServicioService.pagarServicio(any(PagoServicioRequest.class))).thenReturn(mockResponse);

        PagoServicioRequest request = PagoServicioRequest.builder()
                .clienteId(1)
                .numeroCuenta("1234567890")
                .idProducto(185)
                .monto(new BigDecimal("10.00"))
                .referencia("9981234567")
                .idempotencyKey("TX-20261027-001")
                .build();

        mockMvc.perform(post("/pagos/servicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTransaccion").value(101))
                .andExpect(jsonPath("$.idempotencyKey").value("TX-20261027-001"))
                .andExpect(jsonPath("$.estado").value("EXITOSA"))
                .andExpect(jsonPath("$.saldoNuevo").value(490.00));
    }

    @Test
    @DisplayName("POST /pagos/servicio con campos faltantes debe retornar HTTP 400 Bad Request por Bean Validation")
    void pagarServicio_Invalido_Retorna400() throws Exception {
        String jsonInvalido = """
                {
                    "numeroCuenta": "123",
                    "monto": -10.00
                }
                """;

        mockMvc.perform(post("/pagos/servicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("POST /pagos/servicio con campos no reconocidos debe retornar HTTP 400 Bad Request")
    void pagarServicio_CampoDesconocido_Retorna400() throws Exception {
        String jsonConCampoDesconocido = """
                {
                    "clienteId": 1,
                    "numeroCuenta": "1234567890",
                    "idProducto": 185,
                    "monto": 10.00,
                    "referencia": "9981234567",
                    "idempotencyKey": "TX-20261027-001",
                    "campoDesconocido": "inyeccion"
                }
                """;

        mockMvc.perform(post("/pagos/servicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConCampoDesconocido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("Propiedad no reconocida o no permitida: 'campoDesconocido'"));
    }

    @Test
    @DisplayName("POST /pagos/consultar debe retornar HTTP 200 OK con el estado de la transacción")
    void consultarTransaccion_Valido_Retorna200() throws Exception {
        PagoServicioResponse mockResponse = PagoServicioResponse.builder()
                .idTransaccion(101)
                .idempotencyKey("TX-20261027-001")
                .estado("EXITOSA")
                .folioGestoPago("GP-FOLIO-123")
                .build();

        when(pagoServicioService.consultarTransaccion(any(PagoConsultaRequest.class))).thenReturn(mockResponse);

        PagoConsultaRequest request = PagoConsultaRequest.builder()
                .idempotencyKey("TX-20261027-001")
                .build();

        mockMvc.perform(post("/pagos/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("TX-20261027-001"))
                .andExpect(jsonPath("$.estado").value("EXITOSA"));
    }

    @Test
    @DisplayName("POST /pagos/consultar sin idempotencyKey debe retornar HTTP 400 Bad Request")
    void consultarTransaccion_SinClave_Retorna400() throws Exception {
        String jsonVacio = "{}";

        mockMvc.perform(post("/pagos/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonVacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }
}
