package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogoProductoRepository extends JpaRepository<CatalogoProducto, Long> {

    Optional<CatalogoProducto> findByIdProducto(Integer idProducto);

    List<CatalogoProducto> findAllByOrderByServicioAscNombreProductoAsc();

    Optional<CatalogoProducto> findTopByFechaActualizacionIsNotNullOrderByFechaActualizacionDesc();
}
