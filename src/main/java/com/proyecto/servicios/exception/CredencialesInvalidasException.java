package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends NegocioException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
    }

    public CredencialesInvalidasException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
