package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class UsuarioNoEncontradoException extends NegocioException {

    public UsuarioNoEncontradoException(String mensaje) {
        super(mensaje, HttpStatus.NOT_FOUND);
    }

    public UsuarioNoEncontradoException(Integer id) {
        super("Usuario no encontrado con el ID: " + id, HttpStatus.NOT_FOUND);
    }
}
