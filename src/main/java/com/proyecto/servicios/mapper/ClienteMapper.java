package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.DomicilioResponse;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    @Mapping(target = "sexo", source = "sexo", qualifiedByName = "sexoToString")
    @Mapping(target = "estadoCivil", source = "estadoCivil", qualifiedByName = "estadoCivilToString")
    @Mapping(target = "usuarioId", source = "usuario", qualifiedByName = "usuarioToId")
    ClienteResponse toClienteResponse(Cliente cliente);

    List<ClienteResponse> toClienteResponseList(List<Cliente> clientes);

    DomicilioResponse toDomicilioResponse(Domicilio domicilio);

    @Mapping(target = "clienteId", source = "cliente", qualifiedByName = "clienteToId")
    @Mapping(target = "estatus", source = "estatus", qualifiedByName = "estatusToString")
    CuentaResponse toCuentaResponse(Cuenta cuenta);

    List<CuentaResponse> toCuentaResponseList(List<Cuenta> cuentas);

    @Mapping(target = "clienteId", source = "cliente", qualifiedByName = "clienteToId")
    UsuarioResponse toUsuarioResponse(Usuario usuario);

    @Named("sexoToString")
    default String sexoToString(com.proyecto.servicios.entity.onboarding.Sexo sexo) {
        return sexo != null ? sexo.getCodigo() : null;
    }

    @Named("estadoCivilToString")
    default String estadoCivilToString(com.proyecto.servicios.entity.onboarding.EstadoCivil estadoCivil) {
        return estadoCivil != null ? estadoCivil.getCodigo() : null;
    }

    @Named("estatusToString")
    default String estatusToString(com.proyecto.servicios.entity.onboarding.EstatusCuenta estatus) {
        return estatus != null ? estatus.name() : null;
    }

    @Named("usuarioToId")
    default Integer usuarioToId(Usuario usuario) {
        return usuario != null ? usuario.getId() : null;
    }

    @Named("clienteToId")
    default Integer clienteToId(Cliente cliente) {
        return cliente != null ? cliente.getId() : null;
    }
}
