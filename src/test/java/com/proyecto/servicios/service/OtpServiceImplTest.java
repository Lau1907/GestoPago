package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.CodigoAutenticacion;
import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.Codigo2faExpiradoException;
import com.proyecto.servicios.repositorys.onboarding.CodigoAutenticacionRepository;
import com.proyecto.servicios.service.Impl.OtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private CodigoAutenticacionRepository codigoAutenticacionRepository;

    @InjectMocks
    private OtpServiceImpl otpService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1)
                .correo("test@example.com")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("generarCodigoOtp debe generar un código de 6 dígitos numéricos e invalidar anteriores")
    void generarCodigoOtp_Exito() {
        String codigo = otpService.generarCodigoOtp(usuario, TipoDispositivo.WEB);

        assertNotNull(codigo);
        assertEquals(6, codigo.length());
        assertTrue(codigo.matches("^\\d{6}$"));

        verify(codigoAutenticacionRepository, times(1)).invalidarCodigosPrevios(1);
        verify(codigoAutenticacionRepository, times(1)).save(any(CodigoAutenticacion.class));
    }

    @Test
    @DisplayName("validarCodigoOtp debe retornar true y marcar como utilizado cuando el código coincide y está vigente")
    void validarCodigoOtp_Exito() {
        CodigoAutenticacion otp = CodigoAutenticacion.builder()
                .id(10L)
                .usuario(usuario)
                .codigo("123456")
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .utilizado(false)
                .intentos(0)
                .build();

        when(codigoAutenticacionRepository.findTopByUsuarioIdAndUtilizadoFalseOrderByFechaCreacionDesc(1))
                .thenReturn(Optional.of(otp));

        boolean resultado = otpService.validarCodigoOtp(usuario, "123456");

        assertTrue(resultado);
        assertTrue(otp.getUtilizado());
        assertNotNull(otp.getFechaUtilizacion());
        verify(codigoAutenticacionRepository, times(1)).save(otp);
    }

    @Test
    @DisplayName("validarCodigoOtp debe retornar false e incrementar intentos cuando el código es incorrecto")
    void validarCodigoOtp_CodigoIncorrecto() {
        CodigoAutenticacion otp = CodigoAutenticacion.builder()
                .id(10L)
                .usuario(usuario)
                .codigo("123456")
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .utilizado(false)
                .intentos(0)
                .build();

        when(codigoAutenticacionRepository.findTopByUsuarioIdAndUtilizadoFalseOrderByFechaCreacionDesc(1))
                .thenReturn(Optional.of(otp));

        boolean resultado = otpService.validarCodigoOtp(usuario, "999999");

        assertFalse(resultado);
        assertFalse(otp.getUtilizado());
        assertEquals(1, otp.getIntentos());
        verify(codigoAutenticacionRepository, times(1)).save(otp);
    }

    @Test
    @DisplayName("validarCodigoOtp debe lanzar Codigo2faExpiradoException cuando el código ha expirado")
    void validarCodigoOtp_Expirado() {
        CodigoAutenticacion otp = CodigoAutenticacion.builder()
                .id(10L)
                .usuario(usuario)
                .codigo("123456")
                .fechaCreacion(LocalDateTime.now().minusMinutes(10))
                .fechaExpiracion(LocalDateTime.now().minusMinutes(5))
                .utilizado(false)
                .intentos(0)
                .build();

        when(codigoAutenticacionRepository.findTopByUsuarioIdAndUtilizadoFalseOrderByFechaCreacionDesc(1))
                .thenReturn(Optional.of(otp));

        assertThrows(Codigo2faExpiradoException.class, () -> otpService.validarCodigoOtp(usuario, "123456"));
    }
}

