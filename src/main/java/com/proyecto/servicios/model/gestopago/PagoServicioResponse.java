package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta unificada de transacción de pago de servicios")
public class PagoServicioResponse {

    @Schema(description = "Identificador interno de la transacción en nuestra base de datos", example = "101")
    private Integer idTransaccion;

    @Schema(description = "Clave de idempotencia única de la transacción", example = "TX-20261027-0001-ABCD")
    private String idempotencyKey;

    @Schema(description = "Número de cuenta bancaria debitada", example = "1234567890")
    private String numeroCuenta;

    @Schema(description = "Identificador del cliente titular", example = "1")
    private Integer clienteId;

    @Schema(description = "Identificador del producto en GestoPago", example = "185")
    private Integer idProducto;

    @Schema(description = "Nombre del servicio del catálogo", example = "AGUAKAN (Cancun)")
    private String servicio;

    @Schema(description = "Nombre del producto del catálogo", example = "Agua Cancun")
    private String producto;

    @Schema(description = "Monto pagado", example = "10.00")
    private BigDecimal monto;

    @Schema(description = "Saldo de la cuenta antes de la transacción", example = "500.00")
    private BigDecimal saldoAnterior;

    @Schema(description = "Saldo de la cuenta posterior al débito exitoso", example = "490.00")
    private BigDecimal saldoNuevo;

    @Schema(description = "Referencia del servicio pagado", example = "9981234567")
    private String referencia;

    @Schema(description = "Estado de la transacción (EXITOSA, FALLIDA, TIMEOUT_REVISION)", example = "EXITOSA")
    private String estado;

    @Schema(description = "Código de respuesta devuelto por el proveedor externo GestoPago (ej. 01)", example = "01")
    private String codigoGestoPago;

    @Schema(description = "Mensaje textual de respuesta del proveedor externo", example = "Operacion realizada con exito")
    private String mensajeGestoPago;

    @Schema(description = "Folio asignado por GestoPago", example = "GP-20261027-001")
    private String folioGestoPago;

    @Schema(description = "Número de autorización de GestoPago", example = "AUTH987654")
    private String autorizacionGestoPago;

    @Schema(description = "Leyenda o ticket de comprobante emitido por GestoPago")
    private String ticket;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha y hora del registro de la transacción", example = "2026-10-27T16:50:00")
    private LocalDateTime fecha;
}
