package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public NegocioException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public NegocioException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
