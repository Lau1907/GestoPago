package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_autenticacion", indexes = {
        @Index(name = "ix_codigos_autenticacion_usuario", columnList = "usuario_id"),
        @Index(name = "ix_codigos_autenticacion_utilizado", columnList = "utilizado"),
        @Index(name = "ix_codigos_autenticacion_fecha_exp", columnList = "fecha_expiracion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoAutenticacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo", nullable = false, length = 6)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispositivo", nullable = false, length = 20)
    @Builder.Default
    private TipoDispositivo dispositivo = TipoDispositivo.WEB;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "utilizado", nullable = false)
    @Builder.Default
    private Boolean utilizado = false;

    @Column(name = "fecha_utilizacion")
    private LocalDateTime fechaUtilizacion;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.utilizado == null) {
            this.utilizado = false;
        }
        if (this.intentos == null) {
            this.intentos = 0;
        }
        if (this.dispositivo == null) {
            this.dispositivo = TipoDispositivo.WEB;
        }
    }
}

