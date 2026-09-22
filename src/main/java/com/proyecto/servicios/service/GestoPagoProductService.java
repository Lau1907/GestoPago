package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;

public interface GestoPagoProductService {

    /**
     * Obtiene la lista de productos y servicios autorizados desde el servicio externo GestoPago/PuntoRed.
     *
     * @return Objeto GestoPagoProductListResponse deserializado a partir del XML de respuesta
     */
    GestoPagoProductListResponse obtenerProductos();
}
