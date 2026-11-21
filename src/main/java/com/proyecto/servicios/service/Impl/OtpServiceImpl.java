package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.CodigoAutenticacion;
import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.Codigo2faExpiradoException;
import com.proyecto.servicios.repositorys.onboarding.CodigoAutenticacionRepository;
import com.proyecto.servicios.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final CodigoAutenticacionRepository codigoAutenticacionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // Tiempo de vida del código OTP en minutos
    private static final int OTP_EXPIRATION_MINUTES = 5;

    @Override
    @Transactional
    public String generarCodigoOtp(Usuario usuario, TipoDispositivo dispositivo) {
        // 1. Invalidar códigos previos no utilizados
        codigoAutenticacionRepository.invalidarCodigosPrevios(usuario.getId());

        // 2. Generar código numérico criptográficamente seguro de 6 dígitos (100000 - 999999)
        int codigoNum = 100000 + secureRandom.nextInt(900000);
        String codigo = String.valueOf(codigoNum);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiracion = now.plusMinutes(OTP_EXPIRATION_MINUTES);

        CodigoAutenticacion otp = CodigoAutenticacion.builder()
                .usuario(usuario)
                .codigo(codigo)
                .dispositivo(dispositivo != null ? dispositivo : TipoDispositivo.WEB)
                .fechaCreacion(now)
                .fechaExpiracion(expiracion)
                .utilizado(false)
                .intentos(0)
                .build();

        codigoAutenticacionRepository.save(otp);

        log.info("==========================================================================");
        log.info("CÓDIGO OTP 2FA GENERADO PARA [{}]: [{}] | DISPOSITIVO: {} | EXPIRA EN: {} MIN",
                usuario.getCorreo(), codigo, otp.getDispositivo(), OTP_EXPIRATION_MINUTES);
        log.info("==========================================================================");

        return codigo;
    }

    @Override
    @Transactional
    public boolean validarCodigoOtp(Usuario usuario, String codigo) {
        if (codigo == null || codigo.trim().length() != 6) {
            log.warn("Formato de código OTP inválido para usuario ID: {}", usuario.getId());
            return false;
        }

        Optional<CodigoAutenticacion> otpOpt = codigoAutenticacionRepository
                .findTopByUsuarioIdAndUtilizadoFalseOrderByFechaCreacionDesc(usuario.getId());

        if (otpOpt.isEmpty()) {
            log.warn("No existe código OTP activo o vigente para el usuario ID: {}", usuario.getId());
            return false;
        }

        CodigoAutenticacion otp = otpOpt.get();

        // Verificar expiración
        if (otp.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            log.warn("El código OTP para usuario ID: {} ha expirado en {}", usuario.getId(), otp.getFechaExpiracion());
            throw new Codigo2faExpiradoException("El código 2FA ha expirado. Por favor solicite uno nuevo.");
        }

        // Verificar coincidencia
        if (!otp.getCodigo().equals(codigo.trim())) {
            otp.setIntentos(otp.getIntentos() + 1);
            codigoAutenticacionRepository.save(otp);
            log.warn("Código OTP incorrecto ingresado para usuario ID: {}. Intento: {}", usuario.getId(), otp.getIntentos());
            return false;
        }

        // Marcar como utilizado
        otp.setUtilizado(true);
        otp.setFechaUtilizacion(LocalDateTime.now());
        codigoAutenticacionRepository.save(otp);

        log.info("Código OTP 2FA validado exitosamente para usuario ID: {}", usuario.getId());
        return true;
    }
}

