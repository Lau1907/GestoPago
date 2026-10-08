package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.EstatusCuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cuenta por número: {}", numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        return clienteMapper.toCuentaResponse(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> obtenerCuentasActivas() {
        log.info("Consultando todas las cuentas activas");
        List<Cuenta> cuentas = cuentaRepository.findByEstatus(EstatusCuenta.ACTIVA);
        return clienteMapper.toCuentaResponseList(cuentas);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando saldo para cuenta: {}", numeroCuenta);
        return cuentaRepository.findSaldoByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
    }
}
