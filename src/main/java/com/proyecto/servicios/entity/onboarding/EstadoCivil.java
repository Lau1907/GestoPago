package com.proyecto.servicios.entity.onboarding;

import lombok.Getter;

@Getter
public enum EstadoCivil {
    SOLTERO("S", "Soltero/a"),
    CASADO("C", "Casado/a"),
    DIVORCIADO("D", "Divorciado/a"),
    VIUDO("V", "Viudo/a"),
    UNION_LIBRE("U", "Unión Libre");

    private final String codigo;
    private final String descripcion;

    EstadoCivil(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static EstadoCivil fromCodigo(String codigo) {
        if (codigo == null) return null;
        for (EstadoCivil ec : values()) {
            if (ec.getCodigo().equalsIgnoreCase(codigo) || ec.name().equalsIgnoreCase(codigo)) {
                return ec;
            }
        }
        throw new IllegalArgumentException("Código de estado civil no válido: " + codigo);
    }
}
