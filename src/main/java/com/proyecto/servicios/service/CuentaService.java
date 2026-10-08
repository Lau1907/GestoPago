package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.CuentaResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaService {

    CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<CuentaResponse> obtenerCuentasActivas();

    BigDecimal obtenerSaldoPorNumeroCuenta(String numeroCuenta);
}
