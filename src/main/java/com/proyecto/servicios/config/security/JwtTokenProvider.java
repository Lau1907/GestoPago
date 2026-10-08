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
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(algorithm);
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
