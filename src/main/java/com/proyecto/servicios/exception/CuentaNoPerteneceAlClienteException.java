package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoPerteneceAlClienteException extends NegocioException {

    public CuentaNoPerteneceAlClienteException(String numeroCuenta, Integer clienteId) {
        super(String.format("La cuenta bancaria '%s' no pertenece al cliente con ID %d", numeroCuenta, clienteId),
                HttpStatus.BAD_REQUEST);
    }
}
