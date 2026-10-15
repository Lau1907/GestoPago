package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse {

    private Integer codigo;
    private String mensaje;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private List<String> detalles;

    public GenericResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.timestamp = LocalDateTime.now();
    }

    public GenericResponse(Integer codigo, String mensaje, List<String> detalles) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.timestamp = LocalDateTime.now();
        this.detalles = detalles;
    }
}

