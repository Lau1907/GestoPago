package com.proyecto.servicios.entity.onboarding;

import lombok.Getter;

@Getter
public enum Sexo {
    HOMBRE("H", "Hombre"),
    MUJER("M", "Mujer");

    private final String codigo;
    private final String descripcion;

    Sexo(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static Sexo fromCodigo(String codigo) {
        if (codigo == null) return null;
        for (Sexo s : values()) {
            if (s.getCodigo().equalsIgnoreCase(codigo) || s.name().equalsIgnoreCase(codigo)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Código de sexo no válido: " + codigo);
    }
}
