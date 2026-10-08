package com.proyecto.servicios.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.onboarding.ClienteCreateRequest;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteCamposDesconocidosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private GestoPagoTokenService gestoPagoTokenService;

    private DomicilioDto crearDomicilioValido() {
        return DomicilioDto.builder()
                .calle("Av. Hidalgo")
                .numeroExterior("123")
                .colonia("Centro")
                .municipio("Cancun")
                .estado("Quintana Roo")
                .pais("Mexico")
                .codigoPostal("77500")
                .build();
    }

    private ClienteCreateRequest crearClienteCreateRequestValido(String curp, String rfc, String correo) {
        return ClienteCreateRequest.builder()
                .primerNombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .curp(curp)
                .rfc(rfc)
                .correo(correo)
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .telefonoMovil("9981234567")
                .sexo("H")
                .estadoCivil("S")
                .nacionalidad("Mexicana")
                .ocupacion("Ingeniero")
                .empresa("Tech Corp")
                .ingresoMensual(new BigDecimal("25000.00"))
                .domicilio(crearDomicilioValido())
                .password("Prueba#2026")
                .build();
    }

    private String obtenerToken(String correo, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .correo(correo)
                .password(password)
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
    @DisplayName("POST /clientes con saldoInicial en el body debe retornar 400 y no crear cliente, cuenta ni usuario")
    void postCliente_ConSaldoInicial_Retorna400YNoCreaEntidades() throws Exception {
        long countClientesAntes = clienteRepository.count();
        long countCuentasAntes = cuentaRepository.count();
        long countUsuariosAntes = usuarioRepository.count();

        String bodyConSaldoInicial = """
                {
                    "primerNombre": "Carlos",
                    "apellidoPaterno": "Gomez",
                    "apellidoMaterno": "Ruiz",
                    "curp": "HEGG560427MVZRRL04",
                    "rfc": "HEGG560427AB1",
                    "correo": "carlos.gomez@example.com",
                    "fechaNacimiento": "1992-04-10",
                    "telefonoMovil": "9987654321",
                    "sexo": "H",
                    "estadoCivil": "S",
                    "nacionalidad": "Mexicana",
                    "ocupacion": "Contador",
                    "empresa": "Audit S.A.",
                    "ingresoMensual": 20000.00,
                    "domicilio": {
                        "calle": "Av. Hidalgo",
                        "numeroExterior": "123",
                        "colonia": "Centro",
                        "municipio": "Cancun",
                        "estado": "Quintana Roo",
                        "pais": "Mexico",
                        "codigoPostal": "77500"
                    },
                    "password": "Prueba#2026",
                    "saldoInicial": 1000.00
                }
                """;

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConSaldoInicial))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("Propiedad no reconocida o no permitida: 'saldoInicial'"));

        assertEquals(countClientesAntes, clienteRepository.count());
        assertEquals(countCuentasAntes, cuentaRepository.count());
        assertEquals(countUsuariosAntes, usuarioRepository.count());
    }

    @Test
    @DisplayName("PUT /clientes/{id} con curp en el body debe retornar 400 Bad Request indicando que no es modificable")
    void putCliente_ConCurp_Retorna400() throws Exception {
        ClienteCreateRequest request = crearClienteCreateRequestValido("HEGG560427MVZRRL01", "HEGG560427AB2", "test.curp@example.com");
        MvcResult createResult = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int clienteId = createJson.get("id").asInt();
        String token = obtenerToken("test.curp@example.com", "Prueba#2026");

        String bodyPutConCurp = """
                {
                    "primerNombre": "Juan",
                    "apellidoPaterno": "Perez",
                    "apellidoMaterno": "Lopez",
                    "correo": "test.curp@example.com",
                    "fechaNacimiento": "1990-05-15",
                    "telefonoMovil": "9981234567",
                    "sexo": "H",
                    "estadoCivil": "S",
                    "nacionalidad": "Mexicana",
                    "ocupacion": "Ingeniero",
                    "empresa": "Tech Corp",
                    "ingresoMensual": 25000.00,
                    "domicilio": {
                        "calle": "Av. Hidalgo",
                        "numeroExterior": "123",
                        "colonia": "Centro",
                        "municipio": "Cancun",
                        "estado": "Quintana Roo",
                        "pais": "Mexico",
                        "codigoPostal": "77500"
                    },
                    "curp": "HEGG560427MVZRRL09"
                }
                """;

        mockMvc.perform(put("/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPutConCurp))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("El campo 'curp' no es modificable"));
    }

    @Test
    @DisplayName("PUT /clientes/{id} con rfc en el body debe retornar 400 Bad Request indicando que no es modificable")
    void putCliente_ConRfc_Retorna400() throws Exception {
        ClienteCreateRequest request = crearClienteCreateRequestValido("HEGG560427MVZRRL02", "HEGG560427AB3", "test.rfc@example.com");
        MvcResult createResult = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int clienteId = createJson.get("id").asInt();
        String token = obtenerToken("test.rfc@example.com", "Prueba#2026");

        String bodyPutConRfc = """
                {
                    "primerNombre": "Juan",
                    "apellidoPaterno": "Perez",
                    "apellidoMaterno": "Lopez",
                    "correo": "test.rfc@example.com",
                    "fechaNacimiento": "1990-05-15",
                    "telefonoMovil": "9981234567",
                    "sexo": "H",
                    "estadoCivil": "S",
                    "nacionalidad": "Mexicana",
                    "ocupacion": "Ingeniero",
                    "empresa": "Tech Corp",
                    "ingresoMensual": 25000.00,
                    "domicilio": {
                        "calle": "Av. Hidalgo",
                        "numeroExterior": "123",
                        "colonia": "Centro",
                        "municipio": "Cancun",
                        "estado": "Quintana Roo",
                        "pais": "Mexico",
                        "codigoPostal": "77500"
                    },
                    "rfc": "HEGG560427AB9"
                }
                """;

        mockMvc.perform(put("/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPutConRfc))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("El campo 'rfc' no es modificable"));
    }

    @Test
    @DisplayName("PUT /clientes/{id} con un campo desconocido cualquiera debe retornar 400 Bad Request")
    void putCliente_ConCampoDesconocido_Retorna400() throws Exception {
        ClienteCreateRequest request = crearClienteCreateRequestValido("HEGG560427MVZRRL03", "HEGG560427AB4", "test.unknown@example.com");
        MvcResult createResult = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int clienteId = createJson.get("id").asInt();
        String token = obtenerToken("test.unknown@example.com", "Prueba#2026");

        String bodyPutConCampoDesconocido = """
                {
                    "primerNombre": "Juan",
                    "apellidoPaterno": "Perez",
                    "apellidoMaterno": "Lopez",
                    "correo": "test.unknown@example.com",
                    "fechaNacimiento": "1990-05-15",
                    "telefonoMovil": "9981234567",
                    "sexo": "H",
                    "estadoCivil": "S",
                    "nacionalidad": "Mexicana",
                    "ocupacion": "Ingeniero",
                    "empresa": "Tech Corp",
                    "ingresoMensual": 25000.00,
                    "domicilio": {
                        "calle": "Av. Hidalgo",
                        "numeroExterior": "123",
                        "colonia": "Centro",
                        "municipio": "Cancun",
                        "estado": "Quintana Roo",
                        "pais": "Mexico",
                        "codigoPostal": "77500"
                    },
                    "campoDesconocido": "valorInvalido"
                }
                """;

        mockMvc.perform(put("/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPutConCampoDesconocido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("Propiedad no reconocida o no permitida: 'campoDesconocido'"));
    }

    @Test
    @DisplayName("POST y PUT validos deben seguir funcionando correctamente")
    void postYPutValidos_FuncionanCorrectamente() throws Exception {
        ClienteCreateRequest request = crearClienteCreateRequestValido("HEGG560427MVZRRL05", "HEGG560427AB5", "test.valid@example.com");
        MvcResult createResult = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cuentas[0].saldo").value(0.00))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int clienteId = createJson.get("id").asInt();
        String token = obtenerToken("test.valid@example.com", "Prueba#2026");

        String bodyPutValido = """
                {
                    "primerNombre": "Juan Carlos",
                    "apellidoPaterno": "Perez",
                    "apellidoMaterno": "Lopez",
                    "correo": "test.valid@example.com",
                    "fechaNacimiento": "1990-05-15",
                    "telefonoMovil": "9981234567",
                    "sexo": "H",
                    "estadoCivil": "C",
                    "nacionalidad": "Mexicana",
                    "ocupacion": "Arquitecto",
                    "empresa": "Tech Corp",
                    "ingresoMensual": 35000.00,
                    "domicilio": {
                        "calle": "Av. Hidalgo",
                        "numeroExterior": "123",
                        "colonia": "Centro",
                        "municipio": "Cancun",
                        "estado": "Quintana Roo",
                        "pais": "Mexico",
                        "codigoPostal": "77500"
                    }
                }
                """;

        mockMvc.perform(put("/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPutValido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primerNombre").value("Juan Carlos"))
                .andExpect(jsonPath("$.estadoCivil").value("C"))
                .andExpect(jsonPath("$.ocupacion").value("Arquitecto"));
    }
}
