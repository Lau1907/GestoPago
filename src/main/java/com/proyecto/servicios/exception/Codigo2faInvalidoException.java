package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class Codigo2faInvalidoException extends NegocioException {

    public Codigo2faInvalidoException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

