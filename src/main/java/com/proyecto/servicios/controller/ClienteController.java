package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ClienteCreateRequest;
import com.proyecto.servicios.model.onboarding.ClienteFilterRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Registrar nuevo cliente con domicilio, cuenta bancaria automática y usuario")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody ClienteCreateRequest request) {
        ClienteResponse response = clienteService.registrarCliente(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Consultar clientes con soporte de filtros dinámicos (CURP, RFC, correo, cuenta, rango fechas, estatus)")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> obtenerClientes(
            @ModelAttribute ClienteFilterRequest filtro) {
        List<ClienteResponse> response = clienteService.obtenerClientesConFiltro(filtro);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar cliente por ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerClientePorId(@PathVariable Integer id) {
        ClienteResponse response = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @Deprecated(since = "2.0")
    @Operation(summary = "DEPRECATED: Usar GET /clientes?curp={curp} en su lugar")
    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerClientePorCurp(@PathVariable String curp) {
        ClienteResponse response = clienteService.obtenerPorCurp(curp);
        return ResponseEntity.ok(response);
    }

    @Deprecated(since = "2.0")
    @Operation(summary = "DEPRECATED: Usar GET /clientes?rfc={rfc} en su lugar")
    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerClientePorRfc(@PathVariable String rfc) {
        ClienteResponse response = clienteService.obtenerPorRfc(rfc);
        return ResponseEntity.ok(response);
    }

    @Deprecated(since = "2.0")
    @Operation(summary = "DEPRECATED: Usar GET /clientes?numeroCuenta={numeroCuenta} en su lugar")
    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerClientePorNumeroCuenta(@PathVariable String numeroCuenta) {
        ClienteResponse response = clienteService.obtenerPorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(response);
    }

    @Deprecated(since = "2.0")
    @Operation(summary = "DEPRECATED: Usar GET /clientes?correo={correo} en su lugar")
    @GetMapping(value = "/correo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerClientePorCorreo(@RequestParam String correo) {
        ClienteResponse response = clienteService.obtenerPorCorreo(correo);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse response = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
