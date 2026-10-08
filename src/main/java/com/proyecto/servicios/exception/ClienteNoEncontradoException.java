package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteNoEncontradoException extends NegocioException {

    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje, HttpStatus.NOT_FOUND);
    }

    public ClienteNoEncontradoException(Integer id) {
        super("Cliente no encontrado con el ID: " + id, HttpStatus.NOT_FOUND);
    }
}
