package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteYaRegistradoException extends NegocioException {

    public ClienteYaRegistradoException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
