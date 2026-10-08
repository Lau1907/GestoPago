package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ClienteCreateRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteCreateRequest request);

    List<ClienteResponse> obtenerTodos(Boolean activo, LocalDate fechaDesde, LocalDate fechaHasta);

    ClienteResponse obtenerPorId(Integer id);

    ClienteResponse obtenerPorCurp(String curp);

    ClienteResponse obtenerPorRfc(String rfc);

    ClienteResponse obtenerPorCorreo(String correo);

    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> obtenerClientesActivos();

    List<ClienteResponse> obtenerClientesPorRangoFechas(LocalDate desde, LocalDate hasta);

    ClienteResponse actualizarCliente(Integer id, ClienteUpdateRequest request);

    void eliminarCliente(Integer id);
}
