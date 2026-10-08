package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    @Query("SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.domicilio WHERE c.activo = true")
    List<Cliente> findByActivoTrueWithDomicilio();

    @Query("SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.domicilio WHERE c.fechaRegistro BETWEEN :desde AND :hasta")
    List<Cliente> findByFechaRegistroBetweenWithDomicilio(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT c FROM Cliente c JOIN c.cuentas cu WHERE cu.numeroCuenta = :numeroCuenta")
    Optional<Cliente> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    @Query("SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.domicilio")
    List<Cliente> findAllWithDomicilio();
}
