package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends NegocioException {

    public CuentaNoEncontradaException(String numeroCuenta) {
        super("Cuenta no encontrada con el número: " + numeroCuenta, HttpStatus.NOT_FOUND);
    }
}
