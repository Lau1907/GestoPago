package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. Excepciones de Negocio Específicas
    // =========================================================================

    @ExceptionHandler({
            ClienteNoEncontradoException.class,
            CuentaNoEncontradaException.class,
            UsuarioNoEncontradoException.class
    })
    public ResponseEntity<GenericResponse> handleNotFoundExceptions(NegocioException ex) {
        log.warn("Recurso no encontrado (Status {}): {}", ex.getStatus().value(), ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.NOT_FOUND.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            CurpDuplicadaException.class,
            RfcDuplicadoException.class,
            CorreoDuplicadoException.class,
            ClienteYaRegistradoException.class
    })
    public ResponseEntity<GenericResponse> handleDuplicateExceptions(NegocioException ex) {
        log.warn("Recurso duplicado (Status {}): {}", ex.getStatus().value(), ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.CONFLICT.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleCredencialesInvalidasException(CredencialesInvalidasException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.UNAUTHORIZED.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<GenericResponse> handleUsuarioInactivoException(UsuarioInactivoException ex) {
        log.warn("Usuario inactivo: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.FORBIDDEN.value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            ErrorValidacionException.class,
            ContrasenaInvalidaException.class,
            ClienteInactivoException.class
    })
    public ResponseEntity<GenericResponse> handleBadRequestBusinessExceptions(NegocioException ex) {
        log.warn("Regla de negocio inválida (Status {}): {}", ex.getStatus().value(), ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(ex.getStatus().value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<GenericResponse> handleNegocioException(NegocioException ex) {
        log.warn("Excepción de negocio (Status {}): {}", ex.getStatus().value(), ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(ex.getStatus().value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, ex.getStatus());
    }

    // =========================================================================
    // 2. Validaciones Bean Validation y Formato HTTP
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Error de validación de argumentos: {}", ex.getMessage());
        List<String> detalles = new ArrayList<>();
        StringBuilder sb = new StringBuilder("Error de validación en los campos: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String detalle = "[" + error.getField() + ": " + error.getDefaultMessage() + "]";
            detalles.add(detalle);
            sb.append(detalle).append(" ");
        });

        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje(sb.toString().trim())
                .detalles(detalles)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Violación de restricción de parámetros: {}", ex.getMessage());
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(v -> "[" + v.getPropertyPath() + ": " + v.getMessage() + "]")
                .toList();

        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje("Error de validación de parámetros en la petición")
                .detalles(detalles)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Error al leer el cuerpo HTTP de la petición: {}", ex.getMessage());
        Throwable rootCause = ex.getMostSpecificCause();
        String mensaje;
        if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().isBlank()
                && (rootCause.getMessage().startsWith("Propiedad no reconocida")
                || rootCause.getMessage().startsWith("El campo")
                || rootCause instanceof IllegalArgumentException)) {
            mensaje = rootCause.getMessage();
        } else {
            mensaje = "Cuerpo de la petición inválido, mal formado o contiene propiedades no reconocidas";
        }

        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Tipo de parámetro no coincidente: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje("Tipo de parámetro de solicitud inválido: " + ex.getName())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("Violación de integridad de datos en BD: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.CONFLICT.value())
                .mensaje("Conflicto de integridad de datos: el recurso ya existe o viola una restricción del sistema")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.NOT_FOUND.value())
                .mensaje("Recurso no encontrado: " + ex.getResourcePath())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.FORBIDDEN.value())
                .mensaje("Acceso denegado: no cuenta con permisos suficientes para acceder a este recurso")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GenericResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.UNAUTHORIZED.value())
                .mensaje("No autorizado: se requiere autenticación válida")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // 3. Integración Externa y Feign
    // =========================================================================

    @ExceptionHandler(ExternalIntegrationException.class)
    public ResponseEntity<GenericResponse> handleExternalIntegrationException(ExternalIntegrationException ex) {
        log.error("Error en integración con servicio externo: {}", ex.getMessage());
        GenericResponse response = GenericResponse.builder()
                .codigo(ex.getStatus().value())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse> handleFeignException(FeignException ex) {
        log.error("Error de comunicación HTTP vía Feign (Status {}): {}", ex.status(), ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String mensaje;
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.UNAUTHORIZED) {
            mensaje = "Error de autenticación o token expirado en servicio externo";
        } else {
            mensaje = "Fallo en la comunicación con el servicio proveedor externo";
        }

        GenericResponse response = GenericResponse.builder()
                .codigo(status.value())
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, status);
    }

    // =========================================================================
    // 4. Fallback General
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenericException(Exception ex) {
        log.error("Excepción no controlada en la aplicación: ", ex);
        GenericResponse response = GenericResponse.builder()
                .codigo(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .mensaje("Ha ocurrido un error interno en el sistema")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

