package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.proyecto.servicios.model.onboarding.validation.MayorDeEdad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class ClienteUpdateRequest {

    @NotBlank(message = "El primer nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{2,50}$", message = "El primer nombre debe tener entre 2 y 50 caracteres y contener solo letras y espacios")
    private String primerNombre;

    @Pattern(regexp = "^$|^[\\p{L} ]{2,50}$", message = "El segundo nombre debe tener entre 2 y 50 caracteres y contener solo letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{2,50}$", message = "El apellido paterno debe tener entre 2 y 50 caracteres y contener solo letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{2,50}$", message = "El apellido materno debe tener entre 2 y 50 caracteres y contener solo letras y espacios")
    private String apellidoMaterno;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correo;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @MayorDeEdad(message = "El cliente debe ser mayor de edad (18 años cumplidos)")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe tener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    private String telefonoAlternativo;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^[HM]$", message = "El sexo debe ser 'H' u 'M'")
    private String sexo;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "^[SCDVU]$", message = "El estado civil debe ser una clave válida (S, C, D, V, U)")
    private String estadoCivil;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(max = 50, message = "La nacionalidad no puede exceder 50 caracteres")
    private String nacionalidad;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 50, message = "La ocupación no puede exceder 50 caracteres")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 50, message = "La empresa no puede exceder 50 caracteres")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @NotNull(message = "Los datos del domicilio son obligatorios")
    @Valid
    private DomicilioDto domicilio;
}
