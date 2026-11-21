package com.proyecto.servicios.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:}")
    private String configuredSecret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private byte[] secretBytes;
    private Algorithm algorithm;

    @PostConstruct
    public void init() {
        if (configuredSecret == null || configuredSecret.trim().isEmpty()) {
            log.warn("JWT_SECRET no configurada o vacía. Se generará una clave aleatoria temporal para esta sesión de la aplicación.");
            secretBytes = new byte[32];
            new SecureRandom().nextBytes(secretBytes);
        } else {
            byte[] bytes = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                log.warn("JWT_SECRET configurada tiene menos de 32 bytes ({} bytes). Se recomienda una clave de al menos 32 bytes por seguridad.", bytes.length);
            }
            secretBytes = bytes;
        }
        this.algorithm = Algorithm.HMAC256(secretBytes);
    }

    public String generateToken(Integer usuarioId, String correo) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return JWT.create()
                .withSubject(String.valueOf(usuarioId))
                .withClaim("correo", correo)
                .withClaim("dispositivo", "WEB")
                .withClaim("scope", "ACCESS")
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(algorithm);
    }

    public String generateToken(Integer usuarioId, String correo, String dispositivo, String sessionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        var builder = JWT.create()
                .withSubject(String.valueOf(usuarioId))
                .withClaim("correo", correo)
                .withClaim("dispositivo", dispositivo != null ? dispositivo : "WEB")
                .withClaim("scope", "ACCESS")
                .withIssuedAt(now)
                .withExpiresAt(expiryDate);

        if (sessionId != null) {
            builder.withClaim("sessionId", sessionId);
        }

        return builder.sign(algorithm);
    }

    public String generatePreAuthToken(Integer usuarioId, String correo, String dispositivo) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 300000L); // 5 minutos

        return JWT.create()
                .withSubject(String.valueOf(usuarioId))
                .withClaim("correo", correo)
                .withClaim("dispositivo", dispositivo != null ? dispositivo : "WEB")
                .withClaim("scope", "PRE_AUTH")
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(algorithm);
    }

    public DecodedJWT getDecodedJwt(String token) {
        try {
            return JWT.require(algorithm)
                    .build()
                    .verify(token);
        } catch (Exception e) {
            log.debug("Error verificando token JWT: {}", e.getMessage());
            return null;
        }
    }

    public String getSessionIdFromToken(String token) {
        DecodedJWT jwt = getDecodedJwt(token);
        if (jwt != null && !jwt.getClaim("sessionId").isMissing() && !jwt.getClaim("sessionId").isNull()) {
            return jwt.getClaim("sessionId").asString();
        }
        return null;
    }

    public String getDispositivoFromToken(String token) {
        DecodedJWT jwt = getDecodedJwt(token);
        if (jwt != null && !jwt.getClaim("dispositivo").isMissing() && !jwt.getClaim("dispositivo").isNull()) {
            return jwt.getClaim("dispositivo").asString();
        }
        return null;
    }

    public String getScopeFromToken(String token) {
        DecodedJWT jwt = getDecodedJwt(token);
        if (jwt != null && !jwt.getClaim("scope").isMissing() && !jwt.getClaim("scope").isNull()) {
            return jwt.getClaim("scope").asString();
        }
        return null;
    }

    public String getCorreoFromToken(String token) {
        DecodedJWT jwt = getDecodedJwt(token);
        if (jwt != null && !jwt.getClaim("correo").isMissing() && !jwt.getClaim("correo").isNull()) {
            return jwt.getClaim("correo").asString();
        }
        return null;
    }

    public Integer validateAndGetUsuarioId(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm)
                    .build()
                    .verify(token);
            return Integer.parseInt(jwt.getSubject());
        } catch (Exception e) {
            log.debug("Error validando token JWT: {}", e.getMessage());
            return null;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
