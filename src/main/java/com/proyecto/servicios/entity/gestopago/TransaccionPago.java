package com.proyecto.servicios.entity.gestopago;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_pagos", indexes = {
        @Index(name = "ix_transacciones_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "ix_transacciones_cliente_id", columnList = "cliente_id"),
        @Index(name = "ix_transacciones_cuenta_id", columnList = "cuenta_id"),
        @Index(name = "ix_transacciones_fecha_creacion", columnList = "fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "numero_cuenta", nullable = false, length = 10)
    private String numeroCuenta;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "servicio", length = 100)
    private String servicio;

    @Column(name = "producto", length = 100)
    private String producto;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", precision = 14, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(name = "referencia_servicio", nullable = false, length = 50)
    private String referenciaServicio;

    @Column(name = "digito_verificador", length = 10)
    private String digitoVerificador;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 20)
    private EstadoTransaccion estatus;

    @Column(name = "codigo_respuesta_gestopago", length = 10)
    private String codigoRespuestaGestoPago;

    @Column(name = "mensaje_respuesta_gestopago", length = 255)
    private String mensajeRespuestaGestoPago;

    @Column(name = "folio_gestopago", length = 50)
    private String folioGestoPago;

    @Column(name = "autorizacion_gestopago", length = 50)
    private String autorizacionGestoPago;

    @Column(name = "ticket_gestopago", columnDefinition = "TEXT")
    private String ticketGestoPago;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = now;
        }
        this.fechaActualizacion = now;
        if (this.estatus == null) {
            this.estatus = EstadoTransaccion.INICIADA;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
