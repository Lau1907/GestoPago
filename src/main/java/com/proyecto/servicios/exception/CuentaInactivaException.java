package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaInactivaException extends NegocioException {

    public CuentaInactivaException(String numeroCuenta) {
        super(String.format("La cuenta bancaria con número '%s' no se encuentra activa ('A') para procesar pagos", numeroCuenta),
                HttpStatus.BAD_REQUEST);
    }

    public CuentaInactivaException(String message, HttpStatus status) {
        super(message, status);
    }
}
