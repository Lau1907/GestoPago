package com.proyecto.servicios.model.onboarding.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // @NotBlank maneja la nulidad si aplica
        }
        if (password.length() < 8) {
            return false;
        }
        boolean tieneMayuscula = password.chars().anyMatch(Character::isUpperCase);
        boolean tieneMinuscula = password.chars().anyMatch(Character::isLowerCase);
        boolean tieneNumero = password.chars().anyMatch(Character::isDigit);
        boolean tieneEspecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        return tieneMayuscula && tieneMinuscula && tieneNumero && tieneEspecial;
    }
}
