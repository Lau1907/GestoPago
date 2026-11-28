package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalogo_productos", indexes = {
        @Index(name = "ix_catalogo_productos_servicio", columnList = "servicio"),
        @Index(name = "ix_catalogo_productos_id_servicio", columnList = "id_servicio")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_producto", nullable = false, unique = true)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "servicio", nullable = false, length = 255)
    private String servicio;

    @Column(name = "nombre_producto", nullable = false, length = 255)
    private String nombreProducto;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "has_digito_verificador", nullable = false)
    @Builder.Default
    private Boolean hasDigitoVerificador = false;

    @Column(name = "precio", length = 50)
    private String precio;

    @Column(name = "show_ayuda", nullable = false)
    @Builder.Default
    private Boolean showAyuda = false;

    @Column(name = "tipo_referencia", length = 50)
    private String tipoReferencia;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "url_logo", length = 255)
    private String urlLogo;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
