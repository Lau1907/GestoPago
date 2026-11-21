package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.SesionUsuario;
import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;

public interface SesionService {

    SesionUsuario crearNuevaSesion(Usuario usuario, TipoDispositivo dispositivo, String ip, String userAgent);

    boolean isSesionActiva(String tokenId);

    void invalidarSesion(String tokenId, String motivo);
}

