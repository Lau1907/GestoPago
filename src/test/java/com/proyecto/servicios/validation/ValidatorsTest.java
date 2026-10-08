package com.proyecto.servicios.validation;

import com.proyecto.servicios.model.onboarding.validation.MayorDeEdadValidator;
import com.proyecto.servicios.model.onboarding.validation.PasswordSeguraValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorsTest {

    private MayorDeEdadValidator mayorDeEdadValidator;
    private PasswordSeguraValidator passwordSeguraValidator;

    @BeforeEach
    void setUp() {
        mayorDeEdadValidator = new MayorDeEdadValidator();
        passwordSeguraValidator = new PasswordSeguraValidator();
    }

    @Test
    @DisplayName("MayorDeEdadValidator: quien cumple 18 hoy debe ser válido")
    void mayorDeEdad_CumpleHoy_EsValido() {
        LocalDate cumpleHoy = LocalDate.now().minusYears(18);
        assertTrue(mayorDeEdadValidator.isValid(cumpleHoy, null));
    }

    @Test
    @DisplayName("MayorDeEdadValidator: quien cumplió 18 ayer debe ser válido")
    void mayorDeEdad_CumplioAyer_EsValido() {
        LocalDate cumplioAyer = LocalDate.now().minusYears(18).minusDays(1);
        assertTrue(mayorDeEdadValidator.isValid(cumplioAyer, null));
    }

    @Test
    @DisplayName("MayorDeEdadValidator: quien cumple 18 mañana debe ser inválido")
    void mayorDeEdad_CumpleManana_EsInvalido() {
        LocalDate cumpleManana = LocalDate.now().minusYears(18).plusDays(1);
        assertFalse(mayorDeEdadValidator.isValid(cumpleManana, null));
    }

    @Test
    @DisplayName("MayorDeEdadValidator: fecha futura debe ser inválida")
    void mayorDeEdad_FechaFutura_EsInvalido() {
        LocalDate futura = LocalDate.now().plusDays(1);
        assertFalse(mayorDeEdadValidator.isValid(futura, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Prueba#2026", "Password123!", "S3gur4@2026", "M1_Clave$Super"})
    @DisplayName("PasswordSeguraValidator: contraseñas válidas")
    void passwordSegura_Validas(String password) {
        assertTrue(passwordSeguraValidator.isValid(password, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1!", "nomayuscula1!", "NOLOWERCASE1!", "NoSpecialChar123", "   "})
    @DisplayName("PasswordSeguraValidator: contraseñas inválidas")
    void passwordSegura_Invalidas(String password) {
        assertFalse(passwordSeguraValidator.isValid(password, null));
    }

    @Test
    @DisplayName("Validación Regex CURP")
    void regexCurp() {
        String regex = "^[A-Z][AEIOUX][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9]\\d$";
        assertTrue("HEGG560427MVZRRL04".matches(regex));
        assertFalse("INVALID_CURP_123".matches(regex));
    }

    @Test
    @DisplayName("Validación Regex RFC")
    void regexRfc() {
        String regex = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$";
        assertTrue("HEGG560427AB1".matches(regex));
        assertTrue("HEG560427AB1".matches(regex));
        assertFalse("INVALID_RFC".matches(regex));
    }

    @Test
    @DisplayName("Validación Regex Teléfono (10 dígitos)")
    void regexTelefono() {
        String regex = "^\\d{10}$";
        assertTrue("9981234567".matches(regex));
        assertFalse("998123456".matches(regex));
    }

    @Test
    @DisplayName("Validación Regex Código Postal (5 dígitos)")
    void regexCodigoPostal() {
        String regex = "^\\d{5}$";
        assertTrue("77500".matches(regex));
        assertFalse("7750".matches(regex));
    }

    @Test
    @DisplayName("Validación Regex Nombres y Apellidos")
    void regexNombres() {
        String regex = "^[\\p{L} ]{2,50}$";
        assertTrue("María José".matches(regex));
        assertTrue("Pérez González".matches(regex));
        assertFalse("J123".matches(regex));
    }
}
