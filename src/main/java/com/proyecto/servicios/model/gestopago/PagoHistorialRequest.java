package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para consultar historial de transacciones")
public class PagoHistorialRequest {

    @Schema(description = "ID del cliente", example = "1")
    private Integer clienteId;

    @Schema(description = "Número de cuenta bancaria", example = "1000000001")
    private String numeroCuenta;

    @Schema(description = "Filtrar exclusivamente transacciones exitosas", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean soloExitosas = true;
}
