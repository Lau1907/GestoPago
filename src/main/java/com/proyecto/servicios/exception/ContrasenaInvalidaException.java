package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ContrasenaInvalidaException extends NegocioException {

    public ContrasenaInvalidaException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
