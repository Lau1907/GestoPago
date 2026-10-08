package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.onboarding.validation.PasswordSegura;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @PasswordSegura
    private String passwordNueva;
}
