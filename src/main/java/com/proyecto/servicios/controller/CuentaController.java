package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        CuentaResponse response = cuentaService.obtenerPorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CuentaResponse>> obtenerCuentas(
            @RequestParam(required = false, defaultValue = "ACTIVA") String estatus) {
        List<CuentaResponse> response = cuentaService.obtenerCuentasActivas();
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> obtenerSaldo(@PathVariable String numeroCuenta) {
        BigDecimal saldo = cuentaService.obtenerSaldoPorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(Map.of(
                "numeroCuenta", numeroCuenta,
                "saldo", saldo
        ));
    }
}
