package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.Verificacion2faRequest;
import com.proyecto.servicios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación, control de sesión única y 2FA")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Inicio de sesión", description = "Valida credenciales. Si el usuario o dispositivo requiere 2FA, emite un código OTP de 6 dígitos y token temporal.")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validación de código 2FA", description = "Valida el código OTP de 6 dígitos. Si es correcto, invalida cualquier sesión previa activa (Control de Sesión Única) y emite el token JWT final.")
    @PostMapping(value = {"/2fa/verificar", "/verificar-2fa"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> verificar2fa(@Valid @RequestBody Verificacion2faRequest request) {
        LoginResponse response = authService.verificar2fa(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Activar 2FA para usuario", description = "Habilita la autenticación de doble factor para un usuario.")
    @PostMapping(value = "/2fa/activar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> activar2fa(@RequestParam String correo) {
        GenericResponse response = authService.activar2fa(correo);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Desactivar 2FA para usuario", description = "Deshabilita la autenticación de doble factor para un usuario.")
    @PostMapping(value = "/2fa/desactivar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> desactivar2fa(@RequestParam String correo) {
        GenericResponse response = authService.desactivar2fa(correo);
        return ResponseEntity.ok(response);
    }
}
