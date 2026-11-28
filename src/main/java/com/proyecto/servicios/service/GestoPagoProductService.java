package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;

public interface GestoPagoProductService {

    /**
     * Obtiene la lista de productos y servicios persistidos en la base de datos.
     *
     * @return Objeto GestoPagoProductListResponse con los productos y URLs de logotipo completas
     */
    GestoPagoProductListResponse obtenerProductos();

    /**
     * Sincroniza el catálogo desde el servicio externo GestoPago (getProductList.do)
     * respetando el límite máximo de 1 actualización cada 24 horas.
     *
     * @return Objeto GestoPagoProductListResponse actualizado
     */
    GestoPagoProductListResponse sincronizarCatalogoDesdeGestoPago();
}
