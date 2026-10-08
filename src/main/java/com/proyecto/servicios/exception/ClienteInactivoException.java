package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteInactivoException extends NegocioException {

    public ClienteInactivoException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
