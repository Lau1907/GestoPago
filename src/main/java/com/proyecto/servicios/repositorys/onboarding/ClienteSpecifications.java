package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.onboarding.ClienteFilterRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ClienteSpecifications {

    public static Specification<Cliente> conFiltro(ClienteFilterRequest filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Fetch join con Domicilio para evitar consultas N+1 en resultados
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("domicilio", JoinType.LEFT);
            }

            if (filtro != null) {
                if (filtro.getCurp() != null && !filtro.getCurp().isBlank()) {
                    predicates.add(cb.equal(cb.upper(root.get("curp")), filtro.getCurp().trim().toUpperCase()));
                }
                if (filtro.getRfc() != null && !filtro.getRfc().isBlank()) {
                    predicates.add(cb.equal(cb.upper(root.get("rfc")), filtro.getRfc().trim().toUpperCase()));
                }
                if (filtro.getCorreo() != null && !filtro.getCorreo().isBlank()) {
                    predicates.add(cb.equal(cb.lower(root.get("correo")), filtro.getCorreo().trim().toLowerCase()));
                }
                if (filtro.getNumeroCuenta() != null && !filtro.getNumeroCuenta().isBlank()) {
                    Join<Cliente, Cuenta> cuentaJoin = root.join("cuentas", JoinType.INNER);
                    predicates.add(cb.equal(cuentaJoin.get("numeroCuenta"), filtro.getNumeroCuenta().trim()));
                }
                if (filtro.getActivo() != null) {
                    predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
                }
                if (filtro.getFechaDesde() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaDesde().atStartOfDay()));
                }
                if (filtro.getFechaHasta() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaHasta().atTime(LocalTime.MAX)));
                }
                if (filtro.getPrimerNombre() != null && !filtro.getPrimerNombre().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("primerNombre")), "%" + filtro.getPrimerNombre().trim().toLowerCase() + "%"));
                }
                if (filtro.getApellidoPaterno() != null && !filtro.getApellidoPaterno().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + filtro.getApellidoPaterno().trim().toLowerCase() + "%"));
                }
            }

            if (query != null) {
                query.distinct(true);
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
