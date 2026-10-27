package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class TransaccionDuplicadaException extends NegocioException {

    public TransaccionDuplicadaException(String idempotencyKey) {
        super(String.format("Transacción duplicada: la clave de idempotencia '%s' ya se encuentra en procesamiento o en estado de revisión previa", idempotencyKey),
                HttpStatus.CONFLICT);
    }
}
