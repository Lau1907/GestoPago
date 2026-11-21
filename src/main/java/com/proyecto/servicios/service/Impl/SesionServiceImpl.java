package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.security.JwtTokenProvider;
import com.proyecto.servicios.entity.onboarding.SesionUsuario;
import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.repositorys.onboarding.SesionUsuarioRepository;
import com.proyecto.servicios.service.SesionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesionServiceImpl implements SesionService {

    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public SesionUsuario crearNuevaSesion(Usuario usuario, TipoDispositivo dispositivo, String ip, String userAgent) {
        TipoDispositivo disp = dispositivo != null ? dispositivo : TipoDispositivo.WEB;

        // REGLA DE NEGOCIO: CONTROL DE SESIÓN ÚNICA
        // Detectar si existe alguna sesión previa activa para este usuario e invalidarla de inmediato
        List<SesionUsuario> sesionesPrevias = sesionUsuarioRepository.findAllByUsuarioIdAndActivaTrue(usuario.getId());
        for (SesionUsuario previa : sesionesPrevias) {
            log.warn("CONTROL DE SESIÓN ÚNICA: Detectada sesión activa previa [ID: {}, Dispositivo: {}] para usuario ID: {}. Cerrando e invalidando por nuevo login desde [{}]",
                    previa.getId(), previa.getDispositivo(), usuario.getId(), disp);

            previa.setActiva(false);
            previa.setFechaInactivacion(LocalDateTime.now());
            previa.setMotivoInactivacion("SESION_CERRADA_POR_NUEVO_LOGIN_EN_" + disp.name());
        }
        if (!sesionesPrevias.isEmpty()) {
            sesionUsuarioRepository.saveAllAndFlush(sesionesPrevias);
        }

        // Crear nueva sesión activa
        String tokenId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        long duracionMs = jwtTokenProvider != null ? jwtTokenProvider.getExpirationMs() : 3600000L;
        LocalDateTime expiracion = now.plusSeconds(duracionMs / 1000);

        SesionUsuario nuevaSesion = SesionUsuario.builder()
                .usuario(usuario)
                .tokenId(tokenId)
                .dispositivo(disp)
                .activa(true)
                .fechaInicio(now)
                .fechaExpiracion(expiracion)
                .ipOrigen(ip)
                .userAgent(userAgent)
                .build();

        SesionUsuario guardada = sesionUsuarioRepository.saveAndFlush(nuevaSesion);
        log.info("Nueva sesión única iniciada con éxito [ID: {}, TokenId: {}, Dispositivo: {}] para usuario ID: {}",
                guardada.getId(), guardada.getTokenId(), guardada.getDispositivo(), usuario.getId());

        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSesionActiva(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return false;
        }

        Optional<SesionUsuario> sesionOpt = sesionUsuarioRepository.findByTokenId(tokenId);
        if (sesionOpt.isEmpty()) {
            log.debug("No se encontró sesión para tokenId: {}", tokenId);
            return false;
        }

        SesionUsuario sesion = sesionOpt.get();
        if (!Boolean.TRUE.equals(sesion.getActiva())) {
            log.warn("Sesión [ID: {}, Dispositivo: {}] está inactiva (Motivo: {})",
                    sesion.getId(), sesion.getDispositivo(), sesion.getMotivoInactivacion());
            return false;
        }

        if (sesion.getFechaExpiracion() != null && sesion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            log.warn("Sesión [ID: {}] ha expirado temporalmente en {}", sesion.getId(), sesion.getFechaExpiracion());
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public void invalidarSesion(String tokenId, String motivo) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return;
        }

        Optional<SesionUsuario> sesionOpt = sesionUsuarioRepository.findByTokenId(tokenId);
        if (sesionOpt.isPresent()) {
            SesionUsuario sesion = sesionOpt.get();
            sesion.setActiva(false);
            sesion.setFechaInactivacion(LocalDateTime.now());
            sesion.setMotivoInactivacion(motivo != null ? motivo : "INVALIDACION_MANUAL");
            sesionUsuarioRepository.save(sesion);
            log.info("Sesión [ID: {}] invalidada manualmente. Motivo: {}", sesion.getId(), sesion.getMotivoInactivacion());
        }
    }
}

