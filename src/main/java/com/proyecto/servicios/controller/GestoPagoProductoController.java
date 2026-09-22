package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gestopago")
@Slf4j
public class GestoPagoProductoController {

    private final GestoPagoProductService gestoPagoProductService;

    public GestoPagoProductoController(GestoPagoProductService gestoPagoProductService) {
        this.gestoPagoProductService = gestoPagoProductService;
    }

    /**
     * Endpoint REST para consultar la lista de productos y servicios de GestoPago.
     *
     * @return ResponseEntity con la lista de productos y mensaje de estado
     */
    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GestoPagoProductListResponse> obtenerProductos() {
        log.info("Petición recibida en endpoint GET /gestopago/productos");
        GestoPagoProductListResponse response = gestoPagoProductService.obtenerProductos();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
