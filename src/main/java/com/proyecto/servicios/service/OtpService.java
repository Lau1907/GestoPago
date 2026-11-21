package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.TipoDispositivo;
import com.proyecto.servicios.entity.onboarding.Usuario;

public interface OtpService {

    String generarCodigoOtp(Usuario usuario, TipoDispositivo dispositivo);

    boolean validarCodigoOtp(Usuario usuario, String codigo);
}

