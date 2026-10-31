package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Integer> {

    Optional<TransaccionPago> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<TransaccionPago> findByNumeroCuentaOrderByFechaCreacionDesc(String numeroCuenta);

    List<TransaccionPago> findByClienteIdOrderByFechaCreacionDesc(Integer clienteId);
}
