package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProductClient", url = "${gestopago.service.url}")
public interface GestoPagoProductClient {

    /**
     * Consume el servicio externo GET /sistema/service/getProductList.do
     *
     * @param bearerToken Cabecera de autenticación "Bearer <token>"
     * @return Cadena con el contenido de respuesta en formato XML
     */
    @GetMapping("/sistema/service/getProductList.do")
    String getProductList(@RequestHeader("Authorization") String bearerToken);
}
