package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.PasswordChangeRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;

public interface UsuarioService {

    UsuarioResponse obtenerPorId(Integer id, String authenticatedUserId);

    void cambiarPassword(Integer id, PasswordChangeRequest request, String authenticatedUserId);
}
