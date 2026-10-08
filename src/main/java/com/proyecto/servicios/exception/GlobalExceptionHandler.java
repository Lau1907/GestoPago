package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalIntegrationException.class)
    public ResponseEntity<GenericResponse> handleExternalIntegrationException(ExternalIntegrationException ex) {
        log.error("Error en integración con servicio externo: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(ex.getStatus().value());
        response.setMensaje(ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse> handleFeignException(FeignException ex) {
        log.error("Error de comunicación HTTP vía Feign (Status {}): {}", ex.status(), ex.getMessage());
        GenericResponse response = new GenericResponse();
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.UNAUTHORIZED) {
            response.setCodigo(status.value());
            response.setMensaje("Error de autenticación o token expirado en servicio externo");
        } else {
            response.setCodigo(status.value());
            response.setMensaje("Fallo en la comunicación con el servicio proveedor externo");
        }

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<GenericResponse> handleNegocioException(NegocioException ex) {
        log.warn("Excepción de negocio (Status {}): {}", ex.getStatus().value(), ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(ex.getStatus().value());
        response.setMensaje(ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        log.warn("Error de validación de argumentos: {}", ex.getMessage());
        StringBuilder sb = new StringBuilder("Error de validación en los campos: ");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                sb.append("[").append(error.getField()).append(": ").append(error.getDefaultMessage()).append("] ")
        );
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje(sb.toString().trim());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Error al leer el cuerpo HTTP de la petición: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje("Cuerpo de la petición inválido, mal formado o contiene propiedades no reconocidas");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("Tipo de parámetro no coincidente: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje("Tipo de parámetro de solicitud inválido: " + ex.getName());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Violación de integridad de datos en BD: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.CONFLICT.value());
        response.setMensaje("Conflicto de integridad de datos: el recurso ya existe o viola una restricción del sistema");
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.NOT_FOUND.value());
        response.setMensaje("Recurso no encontrado: " + ex.getResourcePath());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenericException(Exception ex) {
        log.error("Excepción no controlada en la aplicación: ", ex);
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMensaje("Ha ocurrido un error interno en el sistema");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
