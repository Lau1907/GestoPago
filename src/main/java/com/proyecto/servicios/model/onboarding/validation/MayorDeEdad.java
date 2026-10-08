package com.proyecto.servicios.model.onboarding.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MayorDeEdadValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDeEdad {

    String message() default "El cliente debe ser mayor de edad (18 años cumplidos)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
