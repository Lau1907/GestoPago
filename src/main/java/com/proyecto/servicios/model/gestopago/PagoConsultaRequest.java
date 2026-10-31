package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "DTO de solicitud para consultar el estado de una transacción de pago de servicio mediante Request Body")
public class PagoConsultaRequest {

    @JsonAnySetter
    public void handleUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("Propiedad no reconocida o no permitida: '" + name + "'");
    }

    @NotBlank(message = "La clave de idempotencia es obligatoria para consultar la transacción")
    @Size(min = 8, max = 64, message = "La clave de idempotencia debe contener entre 8 y 64 caracteres")
    @Schema(description = "Clave de idempotencia única utilizada durante el procesamiento del pago", example = "TX-20261027-0001-ABCD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;
}
