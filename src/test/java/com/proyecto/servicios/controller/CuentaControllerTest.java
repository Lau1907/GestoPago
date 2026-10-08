package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.GlobalExceptionHandler;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CuentaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private CuentaController cuentaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cuentaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /cuentas/{numeroCuenta} debe retornar HTTP 200 OK")
    void obtenerCuenta_Retorna200() throws Exception {
        CuentaResponse response = CuentaResponse.builder()
                .id(1)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .build();

        when(cuentaService.obtenerPorNumeroCuenta("1234567890")).thenReturn(response);

        mockMvc.perform(get("/cuentas/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value("1234567890"))
                .andExpect(jsonPath("$.saldo").value(500.00));
    }

    @Test
    @DisplayName("GET /cuentas/{numeroCuenta}/saldo debe retornar el saldo de la cuenta")
    void obtenerSaldo_Retorna200() throws Exception {
        when(cuentaService.obtenerSaldoPorNumeroCuenta("1234567890")).thenReturn(new BigDecimal("1500.75"));

        mockMvc.perform(get("/cuentas/1234567890/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value("1234567890"))
                .andExpect(jsonPath("$.saldo").value(1500.75));
    }
}
