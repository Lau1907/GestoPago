package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verificacion2faRequest {

    private String correo;

    @NotBlank(message = "El código OTP es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código OTP debe ser numérico de exactamente 6 dígitos")
    private String codigo;

    private String dispositivo; // "WEB" o "MOBILE" (opcional, default "WEB")

    private String tokenTemporal; // Opcional si se utiliza el pre-auth token
}

