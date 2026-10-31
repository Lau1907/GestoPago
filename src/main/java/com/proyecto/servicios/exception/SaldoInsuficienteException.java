package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends NegocioException {

    public SaldoInsuficienteException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public SaldoInsuficienteException(BigDecimal saldoActual, BigDecimal montoRequerido) {
        super(String.format("Saldo insuficiente en la cuenta. Saldo actual: $%s, Monto a pagar requerido: $%s",
                saldoActual != null ? saldoActual.toPlainString() : "0.00",
                montoRequerido != null ? montoRequerido.toPlainString() : "0.00"),
                HttpStatus.BAD_REQUEST);
    }
}
