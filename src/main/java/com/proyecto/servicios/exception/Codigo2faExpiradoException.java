package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class Codigo2faExpiradoException extends NegocioException {

    public Codigo2faExpiradoException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

