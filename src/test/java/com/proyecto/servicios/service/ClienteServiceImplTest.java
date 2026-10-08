package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.*;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteCreateRequest createRequest;
    private Cliente clienteMock;

    @BeforeEach
    void setUp() {
        DomicilioDto domDto = DomicilioDto.builder()
                .calle("Av. Hidalgo")
                .numeroExterior("123")
                .colonia("Centro")
                .municipio("Cancun")
                .estado("Quintana Roo")
                .pais("Mexico")
                .codigoPostal("77500")
                .build();

        createRequest = ClienteCreateRequest.builder()
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
                .domicilio(domDto)
                .password("Prueba#2026")
                .build();

        clienteMock = Cliente.builder()
                .id(1)
                .primerNombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .curp("HEGG560427MVZRRL04")
                .rfc("HEGG560427AB1")
                .correo("juan.perez@example.com")
                .activo(true)
                .cuentas(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("registrarCliente debe crear Cliente, Domicilio, Cuenta y Usuario exitosamente")
    void registrarCliente_Exito() {
        when(clienteRepository.existsByCurp(any())).thenReturn(false);
        when(clienteRepository.existsByRfc(any())).thenReturn(false);
        when(clienteRepository.existsByCorreo(any())).thenReturn(false);
        when(usuarioRepository.existsByCorreo(any())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        when(clienteRepository.save(any())).thenReturn(clienteMock);
        when(clienteMapper.toClienteResponse(any())).thenReturn(ClienteResponse.builder().id(1).curp("HEGG560427MVZRRL04").build());

        ClienteResponse response = clienteService.registrarCliente(createRequest);

        assertNotNull(response);
        assertEquals(1, response.getId());
        verify(clienteRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("registrarCliente debe lanzar CurpDuplicadaException cuando la CURP ya existe")
    void registrarCliente_CurpDuplicada() {
        when(clienteRepository.existsByCurp(any())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.registrarCliente(createRequest));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarCliente debe lanzar RfcDuplicadoException cuando el RFC ya existe")
    void registrarCliente_RfcDuplicado() {
        when(clienteRepository.existsByCurp(any())).thenReturn(false);
        when(clienteRepository.existsByRfc(any())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> clienteService.registrarCliente(createRequest));
    }

    @Test
    @DisplayName("registrarCliente debe lanzar CorreoDuplicadoException cuando el correo ya existe")
    void registrarCliente_CorreoDuplicado() {
        when(clienteRepository.existsByCurp(any())).thenReturn(false);
        when(clienteRepository.existsByRfc(any())).thenReturn(false);
        when(clienteRepository.existsByCorreo(any())).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> clienteService.registrarCliente(createRequest));
    }

    @Test
    @DisplayName("obtenerPorId debe retornar ClienteResponse cuando existe")
    void obtenerPorId_Exito() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));
        when(clienteMapper.toClienteResponse(clienteMock)).thenReturn(ClienteResponse.builder().id(1).build());

        ClienteResponse response = clienteService.obtenerPorId(1);
        assertNotNull(response);
        assertEquals(1, response.getId());
    }

    @Test
    @DisplayName("obtenerPorId debe lanzar ClienteNoEncontradoException cuando no existe")
    void obtenerPorId_NoEncontrado() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorId(99));
    }

    @Test
    @DisplayName("actualizarCliente debe modificar los datos correctamente")
    void actualizarCliente_Exito() {
        ClienteUpdateRequest updateRequest = ClienteUpdateRequest.builder()
                .primerNombre("Juan Carlos")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .correo("juan.perez@example.com")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .telefonoMovil("9987654321")
                .sexo("H")
                .estadoCivil("C")
                .nacionalidad("Mexicana")
                .ocupacion("Director")
                .empresa("Tech Corp")
                .ingresoMensual(new BigDecimal("35000.00"))
                .domicilio(createRequest.getDomicilio())
                .build();

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.save(any())).thenReturn(clienteMock);
        when(clienteMapper.toClienteResponse(any())).thenReturn(ClienteResponse.builder().id(1).primerNombre("Juan Carlos").build());

        ClienteResponse response = clienteService.actualizarCliente(1, updateRequest);
        assertNotNull(response);
        assertEquals("Juan Carlos", response.getPrimerNombre());
    }

    @Test
    @DisplayName("eliminarCliente debe desactivar cliente, cuentas y usuario (baja lógica)")
    void eliminarCliente_Exito() {
        Cuenta cuenta = Cuenta.builder().estatus(EstatusCuenta.ACTIVA).build();
        Usuario usuario = Usuario.builder().activo(true).build();
        clienteMock.setCuentas(List.of(cuenta));
        clienteMock.setUsuario(usuario);

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));

        clienteService.eliminarCliente(1);

        assertFalse(clienteMock.getActivo());
        assertNotNull(clienteMock.getFechaBaja());
        assertEquals(EstatusCuenta.INACTIVA, cuenta.getEstatus());
        assertFalse(usuario.getActivo());
        verify(clienteRepository, times(1)).save(clienteMock);
    }

    @Test
    @DisplayName("init debe lanzar IllegalStateException si la propiedad saldoInicialDefecto es negativa")
    void init_LanzaExcepcionSiSaldoNegativo() {
        org.springframework.test.util.ReflectionTestUtils.setField(clienteService, "saldoInicialDefecto", new BigDecimal("-100.00"));
        assertThrows(IllegalStateException.class, () -> clienteService.init());
    }
}
