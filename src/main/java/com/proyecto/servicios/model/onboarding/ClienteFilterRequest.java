package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Objeto de parámetros para búsqueda y filtrado dinámico de clientes")
public class ClienteFilterRequest {

    @Schema(description = "Filtro por CURP (exacto)", example = "HEGG560427MVZRRL04")
    private String curp;

    @Schema(description = "Filtro por RFC (exacto)", example = "HEGG560427AB1")
    private String rfc;

    @Schema(description = "Filtro por correo electrónico (exacto o case-insensitive)", example = "maria.hernandez@example.com")
    private String correo;

    @Schema(description = "Filtro por número de cuenta bancaria", example = "1234567890")
    private String numeroCuenta;

    @Schema(description = "Filtro por estatus de actividad", example = "true")
    private Boolean activo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha inicial de registro (inclusive)", example = "2026-01-01")
    private LocalDate fechaDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha final de registro (inclusive)", example = "2026-12-31")
    private LocalDate fechaHasta;

    @Schema(description = "Filtro por coincidencia parcial de primer nombre", example = "Juan")
    private String primerNombre;

    @Schema(description = "Filtro por coincidencia parcial de apellido paterno", example = "Perez")
    private String apellidoPaterno;
}
