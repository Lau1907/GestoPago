package com.proyecto.servicios.config;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GestoPagoFeignConfig {

    @Bean
    public Request.Options gestoPagoRequestOptions(
            @Value("${gestopago.service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${gestopago.service.read-timeout-ms}") int readTimeoutMs) {
        return new Request.Options(
                connectTimeoutMs,
                TimeUnit.MILLISECONDS,
                readTimeoutMs,
                TimeUnit.MILLISECONDS,
                true
        );
    }
}
