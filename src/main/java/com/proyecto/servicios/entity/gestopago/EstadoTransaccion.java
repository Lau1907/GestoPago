package com.proyecto.servicios.entity.gestopago;

/**
 * Representa los estados del ciclo de vida de una transacción de pago de servicios.
 */
public enum EstadoTransaccion {
    /**
     * Transacción registrada localmente antes de invocar al proveedor externo.
     */
    INICIADA,

    /**
     * Transacción confirmada exitosamente por GestoPago con débito aplicado.
     */
    EXITOSA,

    /**
     * Transacción rechazada explícitamente por GestoPago (sin afectar saldo).
     */
    FALLIDA,

    /**
     * Transacción en incertidumbre por agotamiento de tiempo de espera (Timeout HTTP 504 / RetryableException).
     * Requiere conciliación o verificación de estatus para evitar pérdidas y cobros duplicados.
     */
    TIMEOUT_REVISION
}
