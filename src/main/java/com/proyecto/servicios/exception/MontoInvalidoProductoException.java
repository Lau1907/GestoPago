package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class MontoInvalidoProductoException extends NegocioException {

    public MontoInvalidoProductoException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
