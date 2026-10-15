package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de petición para operaciones sobre cliente basadas en su identificador único")
public class ClienteIdRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número positivo mayor a cero")
    @Schema(description = "Identificador único numérico del cliente", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;
}
