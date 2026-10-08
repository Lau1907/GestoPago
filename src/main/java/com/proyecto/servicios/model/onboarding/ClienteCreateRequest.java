package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.onboarding.validation.MayorDeEdad;
import com.proyecto.servicios.model.onboarding.validation.PasswordSegura;
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
public class ClienteCreateRequest {

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

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z][AEIOUX][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9]\\d$", message = "La CURP no cumple con el formato oficial de 18 caracteres")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El RFC no cumple con el formato de 12 o 13 caracteres")
    private String rfc;

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

    @NotBlank(message = "La contraseña es obligatoria")
    @PasswordSegura
    private String password;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    private BigDecimal saldoInicial;
}
