package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.service.ClienteService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /clientes debe retornar HTTP 201 CREATED")
    void registrarCliente_Retorna201() throws Exception {
        DomicilioDto dom = DomicilioDto.builder()
                .calle("Av. Hidalgo")
                .numeroExterior("123")
                .colonia("Centro")
                .municipio("Cancun")
                .estado("Quintana Roo")
                .pais("Mexico")
                .codigoPostal("77500")
                .build();

        ClienteCreateRequest request = ClienteCreateRequest.builder()
                .primerNombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .curp("HEGG560427MVZRRL04")
                .rfc("HEGG560427AB1")
                .correo("juan.perez@example.com")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .telefonoMovil("9981234567")
                .sexo("H")
                .estadoCivil("S")
                .nacionalidad("Mexicana")
                .ocupacion("Ingeniero")
                .empresa("Tech Corp")
                .ingresoMensual(new BigDecimal("25000.00"))
                .domicilio(dom)
                .password("Prueba#2026")
                .build();

        ClienteResponse response = ClienteResponse.builder()
                .id(1)
                .primerNombre("Juan")
                .curp("HEGG560427MVZRRL04")
                .activo(true)
                .build();

        when(clienteService.registrarCliente(any())).thenReturn(response);

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.curp").value("HEGG560427MVZRRL04"));
    }

    @Test
    @DisplayName("GET /clientes/{id} debe retornar 200 OK")
    void obtenerPorId_Retorna200() throws Exception {
        ClienteResponse response = ClienteResponse.builder().id(1).primerNombre("Juan").build();
        when(clienteService.obtenerPorId(1)).thenReturn(response);

        mockMvc.perform(get("/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /clientes/{id} debe retornar 404 cuando no existe")
    void obtenerPorId_Retorna404() throws Exception {
        when(clienteService.obtenerPorId(99)).thenThrow(new ClienteNoEncontradoException(99));

        mockMvc.perform(get("/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    @DisplayName("DELETE /clientes/{id} debe retornar HTTP 204 NO CONTENT")
    void eliminarCliente_Retorna204() throws Exception {
        doNothing().when(clienteService).eliminarCliente(1);

        mockMvc.perform(delete("/clientes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /clientes con saldoInicial en el body debe retornar HTTP 400 BAD REQUEST por propiedad no reconocida")
    void registrarCliente_ConSaldoInicialEnBody_Retorna400() throws Exception {
        String jsonConSaldoInicial = """
                {
                    "primerNombre": "Juan",
                    "apellidoPaterno": "Perez",
                    "apellidoMaterno": "Lopez",
                    "curp": "HEGG560427MVZRRL04",
                    "rfc": "HEGG560427AB1",
                    "correo": "juan.perez@example.com",
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
                    "password": "Prueba#2026",
                    "saldoInicial": 500.00
                }
                """;

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConSaldoInicial))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }
}
