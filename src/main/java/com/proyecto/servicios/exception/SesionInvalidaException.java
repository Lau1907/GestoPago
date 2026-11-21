package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SesionInvalidaException extends NegocioException {

    public SesionInvalidaException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

