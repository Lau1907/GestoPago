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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteFiltrosDinamicosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GestoPagoTokenService gestoPagoTokenService;

    private DomicilioDto crearDomicilio() {
        return DomicilioDto.builder()
                .calle("Calle 50")
                .numeroExterior("100")
                .colonia("Centro")
                .municipio("Merida")
                .estado("Yucatan")
                .pais("Mexico")
                .codigoPostal("97000")
                .build();
    }

    private String registrarYObtenerToken(String curp, String rfc, String correo) throws Exception {
        ClienteCreateRequest request = ClienteCreateRequest.builder()
                .primerNombre("Fernando")
                .apellidoPaterno("Castro")
                .apellidoMaterno("Vargas")
                .curp(curp)
                .rfc(rfc)
                .correo(correo)
                .fechaNacimiento(LocalDate.of(1988, 3, 25))
                .telefonoMovil("9991234567")
                .sexo("H")
                .estadoCivil("S")
                .nacionalidad("Mexicana")
                .ocupacion("Desarrollador")
                .empresa("Fintech Innovacion")
                .ingresoMensual(new BigDecimal("40000.00"))
                .domicilio(crearDomicilio())
                .password("Prueba#2026")
                .build();

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .correo(correo)
                .password("Prueba#2026")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("token").asText();
    }

    @Test
    @DisplayName("GET /clientes con filtros dinámicos por CURP, RFC, correo y número de cuenta")
    void consultarClientesConFiltrosDinamicos() throws Exception {
        String curp = "CASF880325HMCRRN01";
        String rfc = "CASF880325AB1";
        String correo = "fernando.castro1@example.com";

        String token = registrarYObtenerToken(curp, rfc, correo);

        // 1. Filtrar por CURP
        MvcResult resCurp = mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .param("curp", curp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].curp").value(curp))
                .andExpect(jsonPath("$[0].correo").value(correo))
                .andReturn();

        JsonNode jsonCurp = objectMapper.readTree(resCurp.getResponse().getContentAsString());
        String numeroCuenta = jsonCurp.get(0).get("cuentas").get(0).get("numeroCuenta").asText();

        // 2. Filtrar por RFC
        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .param("rfc", rfc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rfc").value(rfc));

        // 3. Filtrar por Correo
        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .param("correo", correo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value(correo));

        // 4. Filtrar por Número de Cuenta
        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .param("numeroCuenta", numeroCuenta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].curp").value(curp));

        // 5. Filtrar por Estatus Activo y Rango de Fechas Válido
        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .param("activo", "true")
                        .param("fechaDesde", "2020-01-01")
                        .param("fechaHasta", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 6. Rango de Fechas Inválido (fechaDesde posterior a fechaHasta) retorna 400 con timestamp
        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .param("fechaDesde", "2030-01-01")
                        .param("fechaHasta", "2020-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.mensaje").value("La fecha inicial (fechaDesde) no puede ser posterior a la fecha final (fechaHasta)"));
    }
}
