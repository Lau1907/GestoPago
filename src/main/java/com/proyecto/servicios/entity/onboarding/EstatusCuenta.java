package com.proyecto.servicios.entity.onboarding;

import lombok.Getter;

@Getter
public enum EstatusCuenta {
    ACTIVA("A", "ACTIVA"),
    INACTIVA("I", "INACTIVA");

    private final String codigo;
    private final String descripcion;

    EstatusCuenta(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static EstatusCuenta fromCodigo(String codigo) {
        if (codigo == null) return null;
        for (EstatusCuenta ec : values()) {
            if (ec.getCodigo().equalsIgnoreCase(codigo) || ec.name().equalsIgnoreCase(codigo)) {
                return ec;
            }
        }
        throw new IllegalArgumentException("Código de estatus de cuenta no válido: " + codigo);
    }
}
