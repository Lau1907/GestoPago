package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gestoPagoPaymentClient", url = "${gestopago.service.url}", configuration = GestoPagoFeignConfig.class)
public interface GestoPagoPaymentClient {

    /**
     * Consume el servicio externo transaccional POST /sistema/service/sendTx.do de GestoPago.
     *
     * @param bearerToken       Cabecera de autenticación "Bearer <token>"
     * @param idProducto        Identificador del producto en el catálogo GestoPago
     * @param monto             Monto de la transacción a pagar
     * @param referencia        Número de cuenta, teléfono o contrato del servicio
     * @param digitoVerificador Dígito verificador opcional según catálogo
     * @param tipoFront         Tipo front del catálogo opcional
     * @param idServicio        Identificador del servicio opcional
     * @return Cadena con la respuesta transaccional en formato XML
     */
    @PostMapping(value = "/sistema/service/sendTx.do")
    String sendTx(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam("idProducto") Integer idProducto,
            @RequestParam("monto") String monto,
            @RequestParam("referencia") String referencia,
            @RequestParam(value = "digitoVerificador", required = false) String digitoVerificador,
            @RequestParam(value = "tipoFront", required = false) Integer tipoFront,
            @RequestParam(value = "idServicio", required = false) Integer idServicio
    );
}
