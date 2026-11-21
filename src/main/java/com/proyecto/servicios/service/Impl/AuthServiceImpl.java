package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.security.JwtTokenProvider;
import com.proyecto.servicios.entity.onboarding.SesionUsuario;
import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.Verificacion2faRequest;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.OtpService;
import com.proyecto.servicios.service.SesionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final SesionService sesionService;

    // Constructor primario usado por Spring Boot para inyección completa de dependencias
    @Autowired
    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           OtpService otpService,
                           SesionService sesionService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.otpService = otpService;
        this.sesionService = sesionService;
    }

    // Constructor secundario para compatibilidad estricta con pruebas unitarias heredadas
    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this(usuarioRepository, passwordEncoder, jwtTokenProvider, null, null);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String correoNorm = request.getCorreo().trim().toLowerCase();
        log.info("Intento de inicio de sesión para: {}", correoNorm);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correoNorm);
        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), usuarioOpt.get().getPasswordHash())) {
            log.warn("Fallo de autenticación: credenciales inválidas");
            throw new CredencialesInvalidasException("Credenciales inválidas");
        }

        Usuario usuario = usuarioOpt.get();
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn("Fallo de autenticación: usuario inactivo con ID {}", usuario.getId());
            throw new UsuarioInactivoException("El usuario se encuentra inactivo");
        }

        TipoDispositivo tipoDispositivo = TipoDispositivo.fromString(request.getDispositivo());

        // Se requiere 2FA si:
        // 1. El usuario tiene dosFactoresActivo == true
        // 2. O se especificó requerir2fa == true
        // 3. O se especificó explícitamente el tipo de dispositivo en el request (Requerimiento Unidad II)
        boolean requiere2Fa = (otpService != null) && (
                Boolean.TRUE.equals(usuario.getDosFactoresActivo())
                        || Boolean.TRUE.equals(request.getRequerir2fa())
                        || (request.getDispositivo() != null && !request.getDispositivo().trim().isEmpty())
        );

        if (requiere2Fa) {
            String codigoOtp = otpService.generarCodigoOtp(usuario, tipoDispositivo);
            String preAuthToken = jwtTokenProvider.generatePreAuthToken(usuario.getId(), usuario.getCorreo(), tipoDispositivo.name());

            log.info("2FA requerido para usuario ID: {}. Código OTP generado y token temporal emitido.", usuario.getId());

            return LoginResponse.builder()
                    .token(preAuthToken)
                    .tokenTemporal(preAuthToken)
                    .tipo("PRE_AUTH")
                    .expiraEn(300000L) // 5 minutos
                    .requiere2FA(true)
                    .codigoOtp(codigoOtp)
                    .dispositivo(tipoDispositivo.name())
                    .mensaje("Se requiere validación de 2FA. Ingrese el código OTP de 6 dígitos para completar el acceso.")
                    .build();
        }

        // Flujo directo (sin 2FA):
        String sessionId = null;
        if (sesionService != null) {
            SesionUsuario sesion = sesionService.crearNuevaSesion(usuario, tipoDispositivo, null, null);
            if (sesion != null) {
                sessionId = sesion.getTokenId();
            }
        }

        String token = (sessionId != null)
                ? jwtTokenProvider.generateToken(usuario.getId(), usuario.getCorreo(), tipoDispositivo.name(), sessionId)
                : jwtTokenProvider.generateToken(usuario.getId(), usuario.getCorreo());

        log.info("Inicio de sesión exitoso directo para usuario ID: {}", usuario.getId());

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .expiraEn(jwtTokenProvider.getExpirationMs())
                .requiere2FA(false)
                .dispositivo(tipoDispositivo.name())
                .mensaje("Inicio de sesión exitoso.")
                .build();
    }

    @Override
    @Transactional
    public LoginResponse verificar2fa(Verificacion2faRequest request) {
        String correo = request.getCorreo();

        // Si viene tokenTemporal pero no correo, intentar extraerlo del token
        if ((correo == null || correo.trim().isEmpty()) && StringUtils.hasText(request.getTokenTemporal())) {
            Integer usuarioId = jwtTokenProvider.validateAndGetUsuarioId(request.getTokenTemporal());
            if (usuarioId != null) {
                Usuario u = usuarioRepository.findById(usuarioId)
                        .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));
                correo = u.getCorreo();
            }
        }

        if (correo == null || correo.trim().isEmpty()) {
            throw new ErrorValidacionException("El correo o token temporal es obligatorio para verificar el código 2FA");
        }

        String correoNorm = correo.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreo(correoNorm)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + correoNorm));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException("El usuario se encuentra inactivo");
        }

        if (otpService == null) {
            throw new IllegalStateException("Servicio OTP no disponible");
        }

        boolean esValido = otpService.validarCodigoOtp(usuario, request.getCodigo().trim());
        if (!esValido) {
            log.warn("Código 2FA incorrecto para usuario ID: {}", usuario.getId());
            throw new Codigo2faInvalidoException("Código 2FA inválido o incorrecto");
        }

        // Determinar tipo de dispositivo respetando el request o extrayéndolo del preAuthToken
        TipoDispositivo tipoDispositivo = null;
        if (request.getDispositivo() != null && !request.getDispositivo().trim().isEmpty()) {
            tipoDispositivo = TipoDispositivo.fromString(request.getDispositivo());
        } else if (StringUtils.hasText(request.getTokenTemporal())) {
            String dispFromToken = jwtTokenProvider.getDispositivoFromToken(request.getTokenTemporal());
            if (dispFromToken != null) {
                tipoDispositivo = TipoDispositivo.fromString(dispFromToken);
            }
        }
        if (tipoDispositivo == null) {
            tipoDispositivo = TipoDispositivo.WEB;
        }

        // Control de Sesión Única: Invalidar sesión previa y registrar nueva sesión activa
        String sessionId = null;
        if (sesionService != null) {
            SesionUsuario nuevaSesion = sesionService.crearNuevaSesion(usuario, tipoDispositivo, null, null);
            if (nuevaSesion != null) {
                sessionId = nuevaSesion.getTokenId();
            }
        }

        String token = (sessionId != null)
                ? jwtTokenProvider.generateToken(usuario.getId(), usuario.getCorreo(), tipoDispositivo.name(), sessionId)
                : jwtTokenProvider.generateToken(usuario.getId(), usuario.getCorreo());

        log.info("2FA validado con éxito. Sesión creada para usuario ID: {} (Dispositivo: {})",
                usuario.getId(), tipoDispositivo);

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .expiraEn(jwtTokenProvider.getExpirationMs())
                .requiere2FA(false)
                .dispositivo(tipoDispositivo.name())
                .mensaje("Autenticación de doble factor exitosa. Sesión iniciada correctamente.")
                .build();
    }

    @Override
    @Transactional
    public GenericResponse activar2fa(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new ErrorValidacionException("El correo es obligatorio");
        }
        Usuario usuario = usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + correo));

        usuario.setDosFactoresActivo(true);
        usuarioRepository.save(usuario);
        log.info("2FA activado para usuario ID: {}", usuario.getId());

        return GenericResponse.builder()
                .codigo(200)
                .mensaje("Autenticación de doble factor (2FA) activada correctamente")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public GenericResponse desactivar2fa(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new ErrorValidacionException("El correo es obligatorio");
        }
        Usuario usuario = usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + correo));

        usuario.setDosFactoresActivo(false);
        usuarioRepository.save(usuario);
        log.info("2FA desactivado para usuario ID: {}", usuario.getId());

        return GenericResponse.builder()
                .codigo(200)
                .mensaje("Autenticación de doble factor (2FA) desactivada correctamente")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
