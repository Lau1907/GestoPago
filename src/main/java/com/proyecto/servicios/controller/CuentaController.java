package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.CuentaConsultaRequest;
import com.proyecto.servicios.model.onboarding.CuentaFilterRequest;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.CuentaSaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @Operation(summary = "Consultar cuenta bancaria por número de cuenta mediante Request Body")
    @PostMapping(value = {"/consultar", "/buscar-por-numero"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> obtenerCuentaPorNumero(@Valid @RequestBody CuentaConsultaRequest request) {
        CuentaResponse response = cuentaService.obtenerPorNumeroCuenta(request.getNumeroCuenta());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar cuentas bancarias con filtros dinámicos (estatus) mediante Request Body")
    @PostMapping(value = {"/buscar", "/listar"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CuentaResponse>> obtenerCuentas(
            @Valid @RequestBody(required = false) CuentaFilterRequest filtro) {
        List<CuentaResponse> response = cuentaService.obtenerCuentasActivas();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar saldo de cuenta bancaria mediante Request Body")
    @PostMapping(value = {"/saldo", "/consultar-saldo"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaSaldoResponse> obtenerSaldo(@Valid @RequestBody CuentaConsultaRequest request) {
        BigDecimal saldo = cuentaService.obtenerSaldoPorNumeroCuenta(request.getNumeroCuenta());
        return ResponseEntity.ok(CuentaSaldoResponse.builder()
                .numeroCuenta(request.getNumeroCuenta())
                .saldo(saldo)
                .build());
    }
}
