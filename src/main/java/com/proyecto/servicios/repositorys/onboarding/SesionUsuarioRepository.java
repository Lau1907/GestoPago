package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.SesionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {

    Optional<SesionUsuario> findByTokenId(String tokenId);

    List<SesionUsuario> findAllByUsuarioIdAndActivaTrue(Integer usuarioId);

    @Modifying
    @Query("UPDATE SesionUsuario s SET s.activa = false, s.fechaInactivacion = :fecha, s.motivoInactivacion = :motivo WHERE s.usuario.id = :usuarioId AND s.activa = true")
    int invalidarSesionesActivas(@Param("usuarioId") Integer usuarioId, @Param("fecha") LocalDateTime fecha, @Param("motivo") String motivo);
}

