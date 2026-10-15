package com.proyecto.servicios.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        GenericResponse genericResponse = GenericResponse.builder()
                .codigo(HttpStatus.UNAUTHORIZED.value())
                .mensaje("No autorizado: se requiere un token JWT válido para acceder a este recurso")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        objectMapper.writeValue(response.getOutputStream(), genericResponse);
    }
}
