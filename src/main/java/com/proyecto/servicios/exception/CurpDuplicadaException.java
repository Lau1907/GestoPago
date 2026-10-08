package com.proyecto.servicios.exception;

public class CurpDuplicadaException extends ClienteYaRegistradoException {

    public CurpDuplicadaException(String curp) {
        super("La CURP ya se encuentra registrada en el sistema: " + curp);
    }
}
