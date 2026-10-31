package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.PagoConsultaRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.service.PagoServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el procesamiento y consulta de pagos de servicios GestoPago.
 *
 * <p><strong>ESTÁNDAR DE ARQUITECTURA:</strong>
 * Este controlador cumple estrictamente con el principio de <em>CERO @PathVariable y CERO @RequestParam</em>.
 * Todas las peticiones reciben sus datos de entrada encapsulados en DTOs mediante {@link RequestBody}
 * y son validadas con {@link Valid}.
 */
@RestController
@RequestMapping(value = {"/gestopago/pagos", "/pagos"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos de Servicios GestoPago", description = "Endpoints para el débito bancario y aplicación de pagos en API externa GestoPago")
public class PagoServicioController {

    private final PagoServicioService pagoServicioService;

    @Operation(summary = "Procesar pago de servicios en GestoPago mediante Request Body")
    @PostMapping(
            value = {"/servicio", "/pagar"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PagoServicioResponse> pagarServicio(@Valid @RequestBody PagoServicioRequest request) {
        log.info("Petición REST recibida en endpoint POST /pagos/servicio para cuenta={}, producto={}, monto={}",
                request.getNumeroCuenta(), request.getIdProducto(), request.getMonto());

        PagoServicioResponse response = pagoServicioService.pagarServicio(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar estado de transacción de pago mediante Request Body")
    @PostMapping(
            value = "/consultar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PagoServicioResponse> consultarTransaccion(@Valid @RequestBody PagoConsultaRequest request) {
        log.info("Petición REST recibida en endpoint POST /pagos/consultar para clave={}",
                request.getIdempotencyKey());

        PagoServicioResponse response = pagoServicioService.consultarTransaccion(request);
        return ResponseEntity.ok(response);
    }
}
