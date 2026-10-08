package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.EstatusCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByEstatus(EstatusCuenta estatus);

    List<Cuenta> findByClienteId(Integer clienteId);

    @Query("SELECT c.saldo FROM Cuenta c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<BigDecimal> findSaldoByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);
}
