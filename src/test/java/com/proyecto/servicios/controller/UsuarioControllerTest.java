package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.onboarding.PasswordChangeRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.service.UsuarioService;
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

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    @Mock
    private Principal principal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /usuarios/{id} debe retornar HTTP 200 con la información del usuario")
    void obtenerPorId_Exito() throws Exception {
        UsuarioResponse response = UsuarioResponse.builder()
                .id(10)
                .correo("usuario.prueba@example.com")
                .activo(true)
                .build();

        when(principal.getName()).thenReturn("10");
        when(usuarioService.obtenerPorId(eq(10), eq("10"))).thenReturn(response);

        mockMvc.perform(get("/usuarios/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.correo").value("usuario.prueba@example.com"));
    }

    @Test
    @DisplayName("PUT /usuarios/{id}/password debe retornar HTTP 204 NO CONTENT tras cambiar contraseña")
    void cambiarPassword_Exito() throws Exception {
        PasswordChangeRequest request = PasswordChangeRequest.builder()
                .passwordActual("Prueba#2026")
                .passwordNueva("NuevaPass#2026")
                .build();

        when(principal.getName()).thenReturn("10");
        doNothing().when(usuarioService).cambiarPassword(eq(10), any(), eq("10"));

        mockMvc.perform(put("/usuarios/10/password")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
