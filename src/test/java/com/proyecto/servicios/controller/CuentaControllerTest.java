package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.onboarding.CuentaConsultaRequest;
import com.proyecto.servicios.model.onboarding.CuentaFilterRequest;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.service.CuentaService;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CuentaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private CuentaController cuentaController;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(cuentaController)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /cuentas/consultar debe retornar HTTP 200 OK con CuentaResponse")
    void obtenerCuenta_Retorna200() throws Exception {
        CuentaResponse response = CuentaResponse.builder()
                .id(1)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .build();

        when(cuentaService.obtenerPorNumeroCuenta("1234567890")).thenReturn(response);

        CuentaConsultaRequest request = CuentaConsultaRequest.builder()
                .numeroCuenta("1234567890")
                .build();

        mockMvc.perform(post("/cuentas/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value("1234567890"))
                .andExpect(jsonPath("$.saldo").value(500.00));
    }

    @Test
    @DisplayName("POST /cuentas/saldo debe retornar el saldo en CuentaSaldoResponse")
    void obtenerSaldo_Retorna200() throws Exception {
        when(cuentaService.obtenerSaldoPorNumeroCuenta("1234567890")).thenReturn(new BigDecimal("1500.75"));

        CuentaConsultaRequest request = CuentaConsultaRequest.builder()
                .numeroCuenta("1234567890")
                .build();

        mockMvc.perform(post("/cuentas/saldo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value("1234567890"))
                .andExpect(jsonPath("$.saldo").value(1500.75));
    }

    @Test
    @DisplayName("POST /cuentas/buscar debe retornar lista de cuentas activas")
    void obtenerCuentas_Retorna200() throws Exception {
        CuentaResponse response = CuentaResponse.builder()
                .id(1)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .build();

        when(cuentaService.obtenerCuentasActivas()).thenReturn(List.of(response));

        CuentaFilterRequest filtro = CuentaFilterRequest.builder()
                .estatus("ACTIVA")
                .build();

        mockMvc.perform(post("/cuentas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroCuenta").value("1234567890"));
    }
}
