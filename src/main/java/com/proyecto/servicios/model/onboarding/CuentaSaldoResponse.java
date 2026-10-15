package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de respuesta con información del saldo de la cuenta bancaria")
public class CuentaSaldoResponse {

    @Schema(description = "Número de cuenta bancaria", example = "1234567890")
    private String numeroCuenta;

    @Schema(description = "Saldo disponible actual", example = "1500.50")
    private BigDecimal saldo;
}
