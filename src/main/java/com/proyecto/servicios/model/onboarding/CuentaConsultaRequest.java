package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de petición para consultar cuenta o saldo mediante número de cuenta bancaria")
public class CuentaConsultaRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta bancaria debe contener exactamente 10 dígitos numéricos")
    @Schema(description = "Número de cuenta bancaria de 10 dígitos", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    private String numeroCuenta;
}
