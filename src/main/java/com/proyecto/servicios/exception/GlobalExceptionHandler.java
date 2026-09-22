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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenericException(Exception ex) {
        log.error("Excepción no controlada en la aplicación: ", ex);
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMensaje("Ha ocurrido un error interno en el sistema");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
