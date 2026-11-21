package com.proyecto.servicios.service;

import com.proyecto.servicios.config.security.JwtTokenProvider;
import com.proyecto.servicios.entity.onboarding.SesionUsuario;
import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.repositorys.onboarding.SesionUsuarioRepository;
import com.proyecto.servicios.service.Impl.SesionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionServiceImplTest {

    @Mock
    private SesionUsuarioRepository sesionUsuarioRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private SesionServiceImpl sesionService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1)
                .correo("sesion.test@example.com")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("crearNuevaSesion debe detectar la sesión activa previa (WEB), invalidarla y crear la nueva sesión (MOBILE)")
    void crearNuevaSesion_InvalidaSesionPrevia() {
        // Sesión previa activa en WEB
        SesionUsuario sesionPrevia = SesionUsuario.builder()
                .id(100L)
                .usuario(usuario)
                .tokenId("uuid-web-previa")
                .dispositivo(TipoDispositivo.WEB)
                .activa(true)
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .fechaExpiracion(LocalDateTime.now().plusHours(1))
                .build();

        when(sesionUsuarioRepository.findAllByUsuarioIdAndActivaTrue(1))
                .thenReturn(List.of(sesionPrevia));

        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        when(sesionUsuarioRepository.save(any(SesionUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Nuevo inicio de sesión desde MOBILE
        SesionUsuario nuevaSesion = sesionService.crearNuevaSesion(usuario, TipoDispositivo.MOBILE, "192.168.1.1", "MobileApp/1.0");

        assertNotNull(nuevaSesion);
        assertEquals(TipoDispositivo.MOBILE, nuevaSesion.getDispositivo());
        assertTrue(nuevaSesion.getActiva());

        // Verificar que la sesión previa fue invalidada
        assertFalse(sesionPrevia.getActiva());
        assertNotNull(sesionPrevia.getFechaInactivacion());
        assertTrue(sesionPrevia.getMotivoInactivacion().contains("SESION_CERRADA_POR_NUEVO_LOGIN_EN_MOBILE"));
        verify(sesionUsuarioRepository, atLeast(2)).save(any(SesionUsuario.class));
    }

    @Test
    @DisplayName("isSesionActiva debe retornar true cuando la sesión existe, activa=true y no ha expirado")
    void isSesionActiva_True() {
        SesionUsuario sesion = SesionUsuario.builder()
                .id(1L)
                .tokenId("uuid-token-activo")
                .activa(true)
                .fechaExpiracion(LocalDateTime.now().plusHours(2))
                .build();

        when(sesionUsuarioRepository.findByTokenId("uuid-token-activo"))
                .thenReturn(Optional.of(sesion));

        assertTrue(sesionService.isSesionActiva("uuid-token-activo"));
    }

    @Test
    @DisplayName("isSesionActiva debe retornar false cuando la sesión fue invalidada")
    void isSesionActiva_False_CuandoInactiva() {
        SesionUsuario sesion = SesionUsuario.builder()
                .id(1L)
                .tokenId("uuid-token-invalido")
                .activa(false)
                .motivoInactivacion("SESION_CERRADA_POR_NUEVO_LOGIN")
                .fechaExpiracion(LocalDateTime.now().plusHours(2))
                .build();

        when(sesionUsuarioRepository.findByTokenId("uuid-token-invalido"))
                .thenReturn(Optional.of(sesion));

        assertFalse(sesionService.isSesionActiva("uuid-token-invalido"));
    }

    @Test
    @DisplayName("invalidarSesion debe marcar la sesión como inactiva con el motivo indicado")
    void invalidarSesion_Exito() {
        SesionUsuario sesion = SesionUsuario.builder()
                .id(1L)
                .tokenId("uuid-logout")
                .activa(true)
                .build();

        when(sesionUsuarioRepository.findByTokenId("uuid-logout"))
                .thenReturn(Optional.of(sesion));

        sesionService.invalidarSesion("uuid-logout", "LOGOUT_USUARIO");

        assertFalse(sesion.getActiva());
        assertEquals("LOGOUT_USUARIO", sesion.getMotivoInactivacion());
        verify(sesionUsuarioRepository, times(1)).save(sesion);
    }
}

