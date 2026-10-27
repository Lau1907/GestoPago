package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "DTO de solicitud para procesar un pago de servicio en GestoPago con cargo a cuenta bancaria")
public class PagoServicioRequest {

    @JsonAnySetter
    public void handleUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("Propiedad no reconocida o no permitida: '" + name + "'");
    }

    @NotNull(message = "El ID del cliente titular es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número entero positivo mayor a cero")
    @Schema(description = "Identificador numérico único del cliente titular", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer clienteId;

    @NotBlank(message = "El número de cuenta bancaria es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta bancaria debe contener exactamente 10 dígitos numéricos")
    @Schema(description = "Número de cuenta bancaria de 10 dígitos de donde se debitarán los fondos", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    private String numeroCuenta;

    @NotNull(message = "El identificador de producto de GestoPago es obligatorio")
    @Positive(message = "El ID de producto debe ser un entero positivo")
    @Schema(description = "Identificador del producto en el catálogo de GestoPago", example = "185", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer idProducto;

    @NotNull(message = "El monto a pagar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a pagar debe ser mayor a cero")
    @Digits(integer = 12, fraction = 2, message = "El monto solo puede contener hasta 12 enteros y 2 decimales")
    @Schema(description = "Monto monetario de la transacción en pesos mexicanos (MXN)", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;

    @NotBlank(message = "La referencia del servicio es obligatoria")
    @Size(min = 3, max = 50, message = "La referencia debe contener entre 3 y 50 caracteres")
    @Schema(description = "Número de teléfono, cuenta, contrato o código de barras del servicio a pagar", example = "9981234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String referencia;

    @Pattern(regexp = "^$|^[0-9A-Za-z]{1,10}$", message = "El dígito verificador debe tener máximo 10 caracteres alfanuméricos")
    @Schema(description = "Dígito verificador requerido para algunos productos específicos", example = "0", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String digitoVerificador;

    @NotBlank(message = "La clave de idempotencia es obligatoria para garantizar la unicidad de la transacción")
    @Size(min = 8, max = 64, message = "La clave de idempotencia debe contener entre 8 y 64 caracteres")
    @Schema(description = "UUID o identificador único de transacción generado por el cliente para evitar pagos duplicados", example = "TX-20261027-0001-ABCD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;
}
