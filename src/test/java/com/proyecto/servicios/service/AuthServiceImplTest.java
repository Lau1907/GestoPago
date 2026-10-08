package com.proyecto.servicios.service;

import com.proyecto.servicios.config.security.JwtTokenProvider;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = Usuario.builder()
                .id(10)
                .correo("usuario.prueba@example.com")
                .passwordHash("hashed_pass")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("login debe retornar token cuando credenciales son válidas y usuario está activo")
    void login_Exito() {
        LoginRequest request = LoginRequest.builder()
                .correo("usuario.prueba@example.com")
                .password("Prueba#2026")
                .build();

        when(usuarioRepository.findByCorreo("usuario.prueba@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("Prueba#2026", "hashed_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken(10, "usuario.prueba@example.com")).thenReturn("mocked_jwt_token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked_jwt_token", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals(3600000L, response.getExpiraEn());
    }

    @Test
    @DisplayName("login debe lanzar CredencialesInvalidasException cuando el usuario no existe o la contraseña es incorrecta")
    void login_CredencialesInvalidas() {
        LoginRequest request = LoginRequest.builder()
                .correo("usuario.prueba@example.com")
                .password("WrongPassword")
                .build();

        when(usuarioRepository.findByCorreo("usuario.prueba@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("WrongPassword", "hashed_pass")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("login debe lanzar UsuarioInactivoException cuando el usuario existe pero está inactivo")
    void login_UsuarioInactivo() {
        usuarioMock.setActivo(false);
        LoginRequest request = LoginRequest.builder()
                .correo("usuario.prueba@example.com")
                .password("Prueba#2026")
                .build();

        when(usuarioRepository.findByCorreo("usuario.prueba@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("Prueba#2026", "hashed_pass")).thenReturn(true);

        assertThrows(UsuarioInactivoException.class, () -> authService.login(request));
    }
}
