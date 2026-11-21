package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.service.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /auth/login debe retornar HTTP 200 con el token de acceso")
    void login_Exito() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .correo("usuario.prueba@example.com")
                .password("Prueba#2026")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("jwt.token.fake")
                .tipo("Bearer")
                .expiraEn(3600000L)
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.fake"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login debe retornar HTTP 401 cuando las credenciales son inválidas")
    void login_CredencialesInvalidas() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .correo("usuario.prueba@example.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any())).thenThrow(new CredencialesInvalidasException("Credenciales inválidas"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("POST /auth/2fa/verificar debe retornar HTTP 200 con el token de acceso final")
    void verificar2fa_Exito() throws Exception {
        com.proyecto.servicios.model.onboarding.Verificacion2faRequest request =
                com.proyecto.servicios.model.onboarding.Verificacion2faRequest.builder()
                        .correo("usuario.prueba@example.com")
                        .codigo("123456")
                        .dispositivo("WEB")
                        .build();

        LoginResponse response = LoginResponse.builder()
                .token("jwt.final.token")
                .tipo("Bearer")
                .expiraEn(3600000L)
                .dispositivo("WEB")
                .requiere2FA(false)
                .mensaje("Autenticación de doble factor exitosa.")
                .build();

        when(authService.verificar2fa(any())).thenReturn(response);

        mockMvc.perform(post("/auth/2fa/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.final.token"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.requiere2FA").value(false));
    }

    @Test
    @DisplayName("POST /auth/2fa/verificar debe retornar HTTP 401 cuando el código OTP es incorrecto")
    void verificar2fa_CodigoInvalido() throws Exception {
        com.proyecto.servicios.model.onboarding.Verificacion2faRequest request =
                com.proyecto.servicios.model.onboarding.Verificacion2faRequest.builder()
                        .correo("usuario.prueba@example.com")
                        .codigo("999999")
                        .dispositivo("WEB")
                        .build();

        when(authService.verificar2fa(any())).thenThrow(new com.proyecto.servicios.exception.Codigo2faInvalidoException("Código 2FA inválido o incorrecto"));

        mockMvc.perform(post("/auth/2fa/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.mensaje").value("Código 2FA inválido o incorrecto"));
    }

    @Test
    @DisplayName("POST /auth/2fa/activar debe retornar HTTP 200 con mensaje de confirmación")
    void activar2fa_Exito() throws Exception {
        com.proyecto.servicios.model.GenericResponse genericResponse = com.proyecto.servicios.model.GenericResponse.builder()
                .codigo(200)
                .mensaje("Autenticación de doble factor (2FA) activada correctamente")
                .build();

        when(authService.activar2fa("usuario.prueba@example.com")).thenReturn(genericResponse);

        mockMvc.perform(post("/auth/2fa/activar")
                        .param("correo", "usuario.prueba@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.mensaje").value("Autenticación de doble factor (2FA) activada correctamente"));
    }
}
