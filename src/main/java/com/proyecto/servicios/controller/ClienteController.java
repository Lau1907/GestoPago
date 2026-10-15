package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.*;
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

    @Operation(summary = "Consultar clientes con soporte de filtros dinámicos combinables (CURP, RFC, correo, cuenta, fechas, estatus)")
    @PostMapping(value = "/buscar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> obtenerClientes(
            @Valid @RequestBody(required = false) ClienteFilterRequest filtro) {
        List<ClienteResponse> response = clienteService.obtenerClientesConFiltro(
                filtro != null ? filtro : new ClienteFilterRequest()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar cliente por ID recibido en el Request Body")
    @PostMapping(value = {"/consultar", "/buscar-por-id"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerClientePorId(@Valid @RequestBody ClienteIdRequest request) {
        ClienteResponse response = clienteService.obtenerPorId(request.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar cliente existente mediante Request Body (incluyendo ID del cliente)")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse response = clienteService.actualizarCliente(request.getId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Baja lógica de cliente mediante Request Body (POST)")
    @PostMapping(value = {"/eliminar", "/baja"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> eliminarCliente(@Valid @RequestBody ClienteIdRequest request) {
        clienteService.eliminarCliente(request.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Baja lógica de cliente mediante Request Body (DELETE)")
    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> eliminarClienteDelete(@Valid @RequestBody ClienteIdRequest request) {
        clienteService.eliminarCliente(request.getId());
        return ResponseEntity.noContent().build();
    }
}
