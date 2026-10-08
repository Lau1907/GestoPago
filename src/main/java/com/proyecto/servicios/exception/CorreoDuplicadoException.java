package com.proyecto.servicios.exception;

public class CorreoDuplicadoException extends ClienteYaRegistradoException {

    public CorreoDuplicadoException(String correo) {
        super("El correo electrónico ya se encuentra registrado en el sistema: " + correo);
    }
}
