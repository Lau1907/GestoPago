package com.proyecto.servicios.exception;

public class RfcDuplicadoException extends ClienteYaRegistradoException {

    public RfcDuplicadoException(String rfc) {
        super("El RFC ya se encuentra registrado en el sistema: " + rfc);
    }
}
