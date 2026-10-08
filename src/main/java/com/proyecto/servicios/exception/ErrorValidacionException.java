package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ErrorValidacionException extends NegocioException {

    public ErrorValidacionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
