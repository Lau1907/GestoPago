package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de petición para filtrado y consulta dinámica de cuentas bancarias")
public class CuentaFilterRequest {

    @Schema(description = "Estatus de la cuenta bancaria (ej. ACTIVA, INACTIVA, BLOQUEADA)", example = "ACTIVA")
    private String estatus;
}
