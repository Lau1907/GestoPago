package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:}")
    private String password;

    @Value("${gestopago.auth.bearer-token:}")
    private String fallbackBearerToken;

    @Value("${gestopago.mock.enabled:false}")
    private boolean mockEnabled;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @PostConstruct
    public void init() {
        Optional<GestoPagoToken> existing = tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
        if (existing.isEmpty()) {
            if (StringUtils.isNotBlank(fallbackBearerToken)) {
                log.info("Inicializando token de respaldo/sandbox para distribuidor={} desde configuración", idDistribuidor);
                persistirTokenRespaldo(fallbackBearerToken);
            } else if (mockEnabled || StringUtils.isBlank(password)) {
                log.info("Inicializando token simulado (Modo Mock) para distribuidor={}", idDistribuidor);
                persistirTokenRespaldo("mock-gestopago-jwt-token-distribuidor-" + idDistribuidor);
            }
        }
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:82800000}", initialDelay = 0)
    public void renovarToken() {
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);

        if (mockEnabled && StringUtils.isBlank(password)) {
            log.info("Modo Mock activo sin contraseña configurada: asignando token simulado para distribuidor={}", idDistribuidor);
            persistirTokenRespaldo(StringUtils.isNotBlank(fallbackBearerToken) ? fallbackBearerToken : "mock-gestopago-jwt-token-distribuidor-" + idDistribuidor);
            return;
        }

        if (StringUtils.isBlank(password)) {
            log.warn("La contraseña de GestoPago (gestopago.auth.password) no está configurada o se encuentra vacía");
            aplicarFallbackSiDisponible("Contraseña de integración no configurada");
            return;
        }

        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || StringUtils.isBlank(response.getToken())) {
                String errorMsg = (response != null && StringUtils.isNotBlank(response.getMessage()))
                        ? response.getMessage()
                        : "Respuesta sin token";
                log.error("La respuesta de GestoPago no contiene un token válido: {}", errorMsg);
                aplicarFallbackSiDisponible("Respuesta externa sin token válido: " + errorMsg);
                return;
            }

            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        existing.setActivo(true);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(idDistribuidor);
                        nuevo.setCodigoDispositivo(codigoDispositivo);
                        nuevo.setActivo(true);
                        return nuevo;
                    });

            tokenRepository.save(tokenEntity);
            log.info("Token GestoPago renovado correctamente");

        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage(), e);
            aplicarFallbackSiDisponible("Excepción al conectar con GestoPago: " + e.getMessage());
        }
    }

    private void aplicarFallbackSiDisponible(String motivo) {
        if (StringUtils.isNotBlank(fallbackBearerToken)) {
            log.warn("Activando token de respaldo estático (fallback/sandbox). Motivo: {}", motivo);
            persistirTokenRespaldo(fallbackBearerToken);
        } else if (mockEnabled) {
            log.warn("Activando token simulado (Modo Mock). Motivo: {}", motivo);
            persistirTokenRespaldo("mock-gestopago-jwt-token-distribuidor-" + idDistribuidor);
        } else {
            log.warn("No hay un token de respaldo estático (gestopago.auth.bearer-token) configurado en application.properties");
        }
    }

    private void persistirTokenRespaldo(String tokenValue) {
        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .orElseGet(() -> {
                    GestoPagoToken nuevo = new GestoPagoToken();
                    nuevo.setIdDistribuidor(idDistribuidor);
                    nuevo.setCodigoDispositivo(codigoDispositivo);
                    return nuevo;
                });

        tokenEntity.setToken(tokenValue.startsWith("Bearer ") ? tokenValue.substring(7).trim() : tokenValue.trim());
        tokenEntity.setTokenType("Bearer");
        tokenEntity.setExpiresIn(86400L);
        tokenEntity.setActivo(true);
        tokenRepository.save(tokenEntity);
        log.info("Token para distribuidor={} guardado/actualizado en base de datos", idDistribuidor);
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        Optional<GestoPagoToken> tokenOpt = tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
        if (tokenOpt.isPresent() && Boolean.TRUE.equals(tokenOpt.get().getActivo()) && StringUtils.isNotBlank(tokenOpt.get().getToken())) {
            return tokenOpt;
        }

        if (StringUtils.isNotBlank(fallbackBearerToken)
                && this.idDistribuidor != null && this.idDistribuidor.equals(idDistribuidor)
                && this.codigoDispositivo != null && this.codigoDispositivo.equals(codigoDispositivo)) {
            log.info("Token no disponible en base de datos. Utilizando token de respaldo para distribuidor={}", idDistribuidor);
            persistirTokenRespaldo(fallbackBearerToken);
            return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
        }

        if (mockEnabled
                && this.idDistribuidor != null && this.idDistribuidor.equals(idDistribuidor)
                && this.codigoDispositivo != null && this.codigoDispositivo.equals(codigoDispositivo)) {
            log.info("Modo Mock activo: auto-generando y persistiendo token simulado para distribuidor={}", idDistribuidor);
            persistirTokenRespaldo("mock-gestopago-jwt-token-distribuidor-" + idDistribuidor);
            return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
        }

        return tokenOpt;
    }
}
