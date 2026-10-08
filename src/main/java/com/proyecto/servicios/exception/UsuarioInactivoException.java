package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class UsuarioInactivoException extends NegocioException {

    public UsuarioInactivoException() {
        super("El usuario se encuentra inactivo", HttpStatus.FORBIDDEN);
    }

    public UsuarioInactivoException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
