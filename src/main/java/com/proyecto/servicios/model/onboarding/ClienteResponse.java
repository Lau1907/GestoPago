package com.proyecto.servicios.model.onboarding;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponse {
    private Integer id;
    private String primerNombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String curp;
    private String rfc;
    private String correo;
    private LocalDate fechaNacimiento;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String sexo;
    private String estadoCivil;
    private String nacionalidad;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaBaja;

    private DomicilioResponse domicilio;
    private List<CuentaResponse> cuentas;
    private Integer usuarioId;
}
