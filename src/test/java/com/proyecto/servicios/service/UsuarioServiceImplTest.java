package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.PasswordChangeRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = Usuario.builder()
                .id(10)
                .correo("usuario.prueba@example.com")
                .passwordHash("old_hashed_pass")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("obtenerPorId debe retornar UsuarioResponse cuando el ID coincide con el usuario autenticado")
    void obtenerPorId_Exito() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuarioMock));
        when(clienteMapper.toUsuarioResponse(usuarioMock)).thenReturn(UsuarioResponse.builder().id(10).correo("usuario.prueba@example.com").build());

        UsuarioResponse response = usuarioService.obtenerPorId(10, "10");
        assertNotNull(response);
        assertEquals(10, response.getId());
    }

    @Test
    @DisplayName("obtenerPorId debe lanzar 403 NegocioException cuando intenta ver el perfil de otro usuario")
    void obtenerPorId_AccesoDenegado() {
        assertThrows(NegocioException.class, () -> usuarioService.obtenerPorId(10, "999"));
    }

    @Test
    @DisplayName("cambiarPassword debe actualizar la contraseña cuando la actual es correcta")
    void cambiarPassword_Exito() {
        PasswordChangeRequest request = PasswordChangeRequest.builder()
                .passwordActual("Prueba#2026")
                .passwordNueva("NuevaPass#2026")
                .build();

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("Prueba#2026", "old_hashed_pass")).thenReturn(true);
        when(passwordEncoder.encode("NuevaPass#2026")).thenReturn("new_hashed_pass");

        usuarioService.cambiarPassword(10, request, "10");

        verify(usuarioRepository, times(1)).save(usuarioMock);
        assertEquals("new_hashed_pass", usuarioMock.getPasswordHash());
    }

    @Test
    @DisplayName("cambiarPassword debe lanzar ContrasenaInvalidaException cuando la contraseña actual es incorrecta")
    void cambiarPassword_ContrasenaActualIncorrecta() {
        PasswordChangeRequest request = PasswordChangeRequest.builder()
                .passwordActual("WrongOldPassword")
                .passwordNueva("NuevaPass#2026")
                .build();

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("WrongOldPassword", "old_hashed_pass")).thenReturn(false);

        assertThrows(ContrasenaInvalidaException.class, () -> usuarioService.cambiarPassword(10, request, "10"));
        verify(usuarioRepository, never()).save(any());
    }
}
