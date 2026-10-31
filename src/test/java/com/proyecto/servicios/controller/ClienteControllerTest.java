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
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("POST /clientes/consultar con ID en el body debe retornar 200 OK")
    void obtenerPorId_Retorna200() throws Exception {
        ClienteResponse response = ClienteResponse.builder().id(1).primerNombre("Juan").build();
        when(clienteService.obtenerPorId(1)).thenReturn(response);

        ClienteIdRequest idRequest = ClienteIdRequest.builder().id(1).build();

        mockMvc.perform(post("/clientes/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /clientes/consultar con ID inexistente debe retornar 404")
    void obtenerPorId_Retorna404() throws Exception {
        when(clienteService.obtenerPorId(99)).thenThrow(new ClienteNoEncontradoException(99));

        ClienteIdRequest idRequest = ClienteIdRequest.builder().id(99).build();

        mockMvc.perform(post("/clientes/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    @DisplayName("POST /clientes/eliminar con ID en el body debe retornar HTTP 204 NO CONTENT")
    void eliminarCliente_Retorna204() throws Exception {
        doNothing().when(clienteService).eliminarCliente(1);

        ClienteIdRequest idRequest = ClienteIdRequest.builder().id(1).build();

        mockMvc.perform(post("/clientes/eliminar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /clientes/buscar con filtros dinámicos en el body debe retornar 200 OK y la lista filtrada")
    void obtenerClientes_ConFiltros_Retorna200() throws Exception {
        ClienteResponse c1 = ClienteResponse.builder().id(1).curp("HEGG560427MVZRRL04").activo(true).build();
        when(clienteService.obtenerClientesConFiltro(any())).thenReturn(List.of(c1));

        ClienteFilterRequest filtro = ClienteFilterRequest.builder()
                .curp("HEGG560427MVZRRL04")
                .activo(true)
                .build();

        mockMvc.perform(post("/clientes/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].curp").value("HEGG560427MVZRRL04"));
    }

    @Test
    @DisplayName("POST /clientes/buscar con rango de fechas inválido en el body debe retornar 400 con timestamp")
    void obtenerClientes_RangoFechasInvalido_Retorna400() throws Exception {
        when(clienteService.obtenerClientesConFiltro(any()))
                .thenThrow(new com.proyecto.servicios.exception.ErrorValidacionException("La fecha inicial (fechaDesde) no puede ser posterior a la fecha final (fechaHasta)"));

        ClienteFilterRequest filtro = ClienteFilterRequest.builder()
                .fechaDesde(LocalDate.of(2026, 12, 31))
                .fechaHasta(LocalDate.of(2026, 1, 1))
                .build();

        mockMvc.perform(post("/clientes/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("La fecha inicial (fechaDesde) no puede ser posterior a la fecha final (fechaHasta)"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("PUT /clientes con ID en el body debe retornar HTTP 200 con cliente actualizado")
    void actualizarCliente_Exito_Retorna200() throws Exception {
        DomicilioDto dom = DomicilioDto.builder()
                .calle("Av. Hidalgo")
                .numeroExterior("123")
                .colonia("Centro")
                .municipio("Cancun")
                .estado("Quintana Roo")
                .pais("Mexico")
                .codigoPostal("77500")
                .build();

        ClienteUpdateRequest updateRequest = ClienteUpdateRequest.builder()
                .id(1)
                .primerNombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .correo("juan.actualizado@example.com")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .telefonoMovil("9981234567")
                .sexo("H")
                .estadoCivil("S")
                .nacionalidad("Mexicana")
                .ocupacion("Ingeniero")
                .empresa("Tech Corp")
                .ingresoMensual(new BigDecimal("30000.00"))
                .domicilio(dom)
                .build();

        ClienteResponse response = ClienteResponse.builder()
                .id(1)
                .primerNombre("Juan")
                .correo("juan.actualizado@example.com")
                .build();

        when(clienteService.actualizarCliente(eq(1), any())).thenReturn(response);

        mockMvc.perform(put("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.correo").value("juan.actualizado@example.com"));
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
