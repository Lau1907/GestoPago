package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas", indexes = {
        @Index(name = "ix_cuentas_numero_cuenta", columnList = "numero_cuenta", unique = true),
        @Index(name = "ix_cuentas_cliente_id", columnList = "cliente_id"),
        @Index(name = "ix_cuentas_estatus", columnList = "estatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 10)
    private String numeroCuenta;

    @Column(name = "saldo", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "estatus", nullable = false, length = 1)
    @Builder.Default
    private EstatusCuenta estatus = EstatusCuenta.ACTIVA;

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
        if (this.saldo == null) {
            this.saldo = BigDecimal.ZERO;
        }
        if (this.estatus == null) {
            this.estatus = EstatusCuenta.ACTIVA;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
