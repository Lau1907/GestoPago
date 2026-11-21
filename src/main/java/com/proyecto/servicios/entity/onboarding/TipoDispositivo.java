package com.proyecto.servicios.entity.onboarding;

public enum TipoDispositivo {
    WEB,
    MOBILE;

    public static TipoDispositivo fromString(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return WEB;
        }
        try {
            return TipoDispositivo.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return WEB;
        }
    }
}

