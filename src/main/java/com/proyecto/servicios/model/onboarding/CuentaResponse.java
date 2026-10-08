package com.proyecto.servicios.model.onboarding;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaResponse {
    private Integer id;
    private Integer clienteId;
    private String numeroCuenta;
    private BigDecimal saldo;
    private String estatus;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
