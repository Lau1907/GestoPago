package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.Verificacion2faRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse verificar2fa(Verificacion2faRequest request);

    GenericResponse activar2fa(String correo);

    GenericResponse desactivar2fa(String correo);
}
