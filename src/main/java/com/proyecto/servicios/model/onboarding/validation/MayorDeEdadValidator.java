package com.proyecto.servicios.model.onboarding.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class MayorDeEdadValidator implements ConstraintValidator<MayorDeEdad, LocalDate> {

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        if (fechaNacimiento == null) {
            return true; // @NotNull debe manejar la obligatoriedad si aplica
        }
        LocalDate hoy = LocalDate.now();
        if (fechaNacimiento.isAfter(hoy)) {
            return false;
        }
        // Quien cumple 18 hoy sí cuenta: fechaNacimiento.plusYears(18) no debe ser posterior a hoy
        return !fechaNacimiento.plusYears(18).isAfter(hoy);
    }
}
