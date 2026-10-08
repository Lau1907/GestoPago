package com.proyecto.servicios.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.onboarding.ClienteCreateRequest;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OnboardingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GestoPagoTokenService gestoPagoTokenService;

    @Test
    @DisplayName("Flujo de integración completo: registrar -> login -> consulta con token -> consulta sin token -> baja lógica -> token invalidad -> login denegado")
    void flujoCompletoOnboarding() throws Exception {
        // 1. Registrar Cliente
        DomicilioDto dom = DomicilioDto.builder()
                .calle("Av. Tulum")
                .numeroExterior("45")
                .colonia("Centro")
                .municipio("Benito Juarez")
                .estado("Quintana Roo")
                .pais("Mexico")
                .codigoPostal("77500")
                .build();

        ClienteCreateRequest createRequest = ClienteCreateRequest.builder()
                .primerNombre("Maria")
                .apellidoPaterno("Hernandez")
                .apellidoMaterno("Gomez")
                .curp("HEGG560427MVZRRL04")
                .rfc("HEGG560427AB1")
                .correo("maria.hernandez@example.com")
                .fechaNacimiento(LocalDate.of(1995, 8, 20))
                .telefonoMovil("9989876543")
                .sexo("M")
                .estadoCivil("S")
                .nacionalidad("Mexicana")
                .ocupacion("Consultora")
                .empresa("Fintech S.A.")
                .ingresoMensual(new BigDecimal("30000.00"))
                .domicilio(dom)
                .password("Prueba#2026")
                .saldoInicial(new BigDecimal("500.00"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.usuarioId").exists())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int clienteId = createJson.get("id").asInt();

        // 2. Login para obtener token
        LoginRequest loginRequest = LoginRequest.builder()
                .correo("maria.hernandez@example.com")
                .password("Prueba#2026")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("token").asText();

        // 3. Consultar cliente CON token (exitoso 200 OK)
        mockMvc.perform(get("/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primerNombre").value("Maria"));

        // 4. Consultar sin token (falla 401 Unauthorized)
        mockMvc.perform(get("/clientes/" + clienteId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));

        // 5. Baja lógica del cliente (usando el token vigente)
        mockMvc.perform(delete("/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 6. Intentar consultar con el MISMO token después de la baja (falla 401 porque el usuario quedó inactivo en BD)
        mockMvc.perform(get("/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));

        // 7. Intentar nuevo login de usuario inactivo (falla 403 Forbidden)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403))
                .andExpect(jsonPath("$.mensaje").value("El usuario se encuentra inactivo"));
    }
}
