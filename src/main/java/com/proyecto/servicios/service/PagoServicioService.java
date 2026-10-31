package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.PagoConsultaRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;

/**
 * Servicio de negocio para la orquestación y procesamiento seguro de pagos de servicios
 * conectados a la API externa de GestoPago (sendTx.do).
 */
public interface PagoServicioService {

    /**
     * Procesa un pago de servicios debitando los fondos de la cuenta del cliente
     * y consumiendo el servicio externo POST /sistema/service/sendTx.do de GestoPago.
     *
     * @param request Datos de la transacción validados mediante Request Body
     * @return Detalle de la transacción procesada con saldos actualizados y folio
     */
    PagoServicioResponse pagarServicio(PagoServicioRequest request);

    /**
     * Consulta el estado de una transacción previa a través de su clave de idempotencia
     * mediante Request Body.
     *
     * @param request Datos de consulta validados mediante Request Body
     * @return Detalle de la transacción registrada
     */
    PagoServicioResponse consultarTransaccion(PagoConsultaRequest request);
}
