package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    private String dispositivo; // "WEB" o "MOBILE" (opcional, default "WEB")

    private Boolean requerir2fa; // Forzado de 2FA si se especifica
}
