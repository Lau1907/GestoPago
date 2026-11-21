package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.CodigoAutenticacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoAutenticacionRepository extends JpaRepository<CodigoAutenticacion, Long> {

    Optional<CodigoAutenticacion> findTopByUsuarioIdAndUtilizadoFalseOrderByFechaCreacionDesc(Integer usuarioId);

    List<CodigoAutenticacion> findAllByUsuarioIdAndUtilizadoFalse(Integer usuarioId);

    @Modifying
    @Query("UPDATE CodigoAutenticacion c SET c.utilizado = true WHERE c.usuario.id = :usuarioId AND c.utilizado = false")
    int invalidarCodigosPrevios(@Param("usuarioId") Integer usuarioId);
}

