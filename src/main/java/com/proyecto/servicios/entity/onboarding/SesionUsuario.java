package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_usuarios", indexes = {
        @Index(name = "ix_sesiones_usuario_id", columnList = "usuario_id"),
        @Index(name = "ix_sesiones_activa", columnList = "activa"),
        @Index(name = "ix_sesiones_token_id", columnList = "token_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    private String tokenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispositivo", nullable = false, length = 20)
    private TipoDispositivo dispositivo;

    @Column(name = "activa", nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_inactivacion")
    private LocalDateTime fechaInactivacion;

    @Column(name = "motivo_inactivacion", length = 150)
    private String motivoInactivacion;

    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @PrePersist
    public void prePersist() {
        if (this.fechaInicio == null) {
            this.fechaInicio = LocalDateTime.now();
        }
        if (this.activa == null) {
            this.activa = true;
        }
        if (this.dispositivo == null) {
            this.dispositivo = TipoDispositivo.WEB;
        }
    }
}

