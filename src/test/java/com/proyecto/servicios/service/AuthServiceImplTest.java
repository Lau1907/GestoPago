package com.proyecto.servicios.service;

import com.proyecto.servicios.config.security.JwtTokenProvider;
import com.proyecto.servicios.entity.onboarding.SesionUsuario;
import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.Codigo2faInvalidoException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.Verificacion2faRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OtpService otpService;

    @Mock
    private SesionService sesionService;

    @Mock
    private EmailService emailService;

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
                .dosFactoresActivo(false)
                .build();
    }

    @Test
    @DisplayName("login debe retornar token cuando credenciales son válidas y usuario está activo sin 2FA")
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
        assertFalse(Boolean.TRUE.equals(response.getRequiere2FA()));
    }

    @Test
    @DisplayName("login debe requerir 2FA y generar código OTP cuando el usuario tiene 2FA activo o especifica dispositivo")
    void login_Con2fa_RetornaPreAuth() {
        usuarioMock.setDosFactoresActivo(true);
        LoginRequest request = LoginRequest.builder()
                .correo("usuario.prueba@example.com")
                .password("Prueba#2026")
                .dispositivo("WEB")
                .build();

        when(usuarioRepository.findByCorreo("usuario.prueba@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("Prueba#2026", "hashed_pass")).thenReturn(true);
        when(otpService.generarCodigoOtp(usuarioMock, TipoDispositivo.WEB)).thenReturn("654321");
        when(jwtTokenProvider.generatePreAuthToken(10, "usuario.prueba@example.com", "WEB")).thenReturn("pre_auth_token_mock");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.getRequiere2FA());
        assertEquals("PRE_AUTH", response.getTipo());
        assertEquals("pre_auth_token_mock", response.getToken());
        assertEquals("WEB", response.getDispositivo());
        verify(emailService).enviarCodigoOtp("usuario.prueba@example.com", "654321", 5);
    }

    @Test
    @DisplayName("verificar2fa debe retornar token Bearer final y registrar sesión cuando el código OTP es correcto")
    void verificar2fa_Exito() {
        Verificacion2faRequest request = Verificacion2faRequest.builder()
                .correo("usuario.prueba@example.com")
                .codigo("654321")
                .dispositivo("MOBILE")
                .build();

        SesionUsuario sesionMock = SesionUsuario.builder()
                .id(1L)
                .tokenId("uuid-sesion-mobile-123")
                .usuario(usuarioMock)
                .dispositivo(TipoDispositivo.MOBILE)
                .activa(true)
                .build();

        when(usuarioRepository.findByCorreo("usuario.prueba@example.com")).thenReturn(Optional.of(usuarioMock));
        when(otpService.validarCodigoOtp(usuarioMock, "654321")).thenReturn(true);
        when(sesionService.crearNuevaSesion(usuarioMock, TipoDispositivo.MOBILE, null, null)).thenReturn(sesionMock);
        when(jwtTokenProvider.generateToken(10, "usuario.prueba@example.com", "MOBILE", "uuid-sesion-mobile-123"))
                .thenReturn("jwt_final_access_token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.verificar2fa(request);

        assertNotNull(response);
        assertEquals("jwt_final_access_token", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("MOBILE", response.getDispositivo());
        assertFalse(response.getRequiere2FA());
    }

    @Test
    @DisplayName("verificar2fa debe lanzar Codigo2faInvalidoException cuando el OTP no es válido")
    void verificar2fa_CodigoInvalido() {
        Verificacion2faRequest request = Verificacion2faRequest.builder()
                .correo("usuario.prueba@example.com")
                .codigo("000000")
                .dispositivo("WEB")
                .build();

        when(usuarioRepository.findByCorreo("usuario.prueba@example.com")).thenReturn(Optional.of(usuarioMock));
        when(otpService.validarCodigoOtp(usuarioMock, "000000")).thenReturn(false);

        assertThrows(Codigo2faInvalidoException.class, () -> authService.verificar2fa(request));
    }

    @Test
    @DisplayName("activar2fa y desactivar2fa deben actualizar el estado en el usuario")
    void activarYDesactivar2fa_Exito() {
        when(usuarioRepository.findByCorreo("usuario.prueba@example.com")).thenReturn(Optional.of(usuarioMock));

        GenericResponse activaResp = authService.activar2fa("usuario.prueba@example.com");
        assertEquals(200, activaResp.getCodigo());
        assertTrue(usuarioMock.getDosFactoresActivo());

        GenericResponse desactivaResp = authService.desactivar2fa("usuario.prueba@example.com");
        assertEquals(200, desactivaResp.getCodigo());
        assertFalse(usuarioMock.getDosFactoresActivo());
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
