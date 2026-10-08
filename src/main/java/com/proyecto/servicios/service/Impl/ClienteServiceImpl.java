package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.*;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteCreateRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${onboarding.cuenta.saldo-inicial:0.00}")
    private BigDecimal saldoInicialDefecto;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteCreateRequest request) {
        log.info("Iniciando proceso de registro para nuevo cliente");

        String curpNorm = request.getCurp().trim().toUpperCase();
        String rfcNorm = request.getRfc().trim().toUpperCase();
        String correoNorm = request.getCorreo().trim().toLowerCase();

        if (clienteRepository.existsByCurp(curpNorm)) {
            log.warn("Intento de registro fallido: CURP duplicada");
            throw new CurpDuplicadaException(curpNorm);
        }
        if (clienteRepository.existsByRfc(rfcNorm)) {
            log.warn("Intento de registro fallido: RFC duplicado");
            throw new RfcDuplicadoException(rfcNorm);
        }
        if (clienteRepository.existsByCorreo(correoNorm) || usuarioRepository.existsByCorreo(correoNorm)) {
            log.warn("Intento de registro fallido: Correo duplicado");
            throw new CorreoDuplicadoException(correoNorm);
        }

        Cliente cliente = Cliente.builder()
                .primerNombre(request.getPrimerNombre().trim())
                .segundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null)
                .apellidoPaterno(request.getApellidoPaterno().trim())
                .apellidoMaterno(request.getApellidoMaterno().trim())
                .curp(curpNorm)
                .rfc(rfcNorm)
                .correo(correoNorm)
                .fechaNacimiento(request.getFechaNacimiento())
                .telefonoMovil(request.getTelefonoMovil().trim())
                .telefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().trim().isEmpty()
                        ? request.getTelefonoAlternativo().trim() : null)
                .sexo(Sexo.fromCodigo(request.getSexo()))
                .estadoCivil(EstadoCivil.fromCodigo(request.getEstadoCivil()))
                .nacionalidad(request.getNacionalidad().trim())
                .ocupacion(request.getOcupacion().trim())
                .empresa(request.getEmpresa().trim())
                .ingresoMensual(request.getIngresoMensual())
                .activo(true)
                .build();

        DomicilioDto domDto = request.getDomicilio();
        Domicilio domicilio = Domicilio.builder()
                .cliente(cliente)
                .calle(domDto.getCalle().trim())
                .numeroExterior(domDto.getNumeroExterior().trim())
                .numeroInterior(domDto.getNumeroInterior() != null ? domDto.getNumeroInterior().trim() : null)
                .colonia(domDto.getColonia().trim())
                .municipio(domDto.getMunicipio().trim())
                .estado(domDto.getEstado().trim())
                .pais(domDto.getPais().trim())
                .codigoPostal(domDto.getCodigoPostal().trim())
                .build();
        cliente.setDomicilio(domicilio);

        BigDecimal saldoInicial = request.getSaldoInicial() != null ? request.getSaldoInicial() : saldoInicialDefecto;
        String numeroCuenta = generarNumeroCuentaUnico();

        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .saldo(saldoInicial)
                .estatus(EstatusCuenta.ACTIVA)
                .build();
        cliente.getCuentas().add(cuenta);

        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(correoNorm)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .build();
        cliente.setUsuario(usuario);

        Cliente clienteGuardado = clienteRepository.save(cliente);
        log.info("Cliente registrado exitosamente con ID: {}", clienteGuardado.getId());

        return clienteMapper.toClienteResponse(clienteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerTodos(Boolean activo, LocalDate fechaDesde, LocalDate fechaHasta) {
        log.info("Consultando clientes con filtros - activo: {}, fechaDesde: {}, fechaHasta: {}", activo, fechaDesde, fechaHasta);

        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ErrorValidacionException("La fecha inicial (fechaDesde) no puede ser posterior a la fecha final (fechaHasta)");
        }

        List<Cliente> lista;
        if (fechaDesde != null && fechaHasta != null) {
            LocalDateTime desde = fechaDesde.atStartOfDay();
            LocalDateTime hasta = fechaHasta.atTime(LocalTime.MAX);
            lista = clienteRepository.findByFechaRegistroBetweenWithDomicilio(desde, hasta);
            if (activo != null) {
                lista = lista.stream().filter(c -> c.getActivo().equals(activo)).toList();
            }
        } else if (Boolean.TRUE.equals(activo)) {
            lista = clienteRepository.findByActivoTrueWithDomicilio();
        } else {
            lista = clienteRepository.findAllWithDomicilio();
            if (activo != null) {
                lista = lista.stream().filter(c -> c.getActivo().equals(activo)).toList();
            }
        }
        return clienteMapper.toClienteResponseList(lista);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Integer id) {
        log.info("Consultando cliente por ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
        return clienteMapper.toClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCurp(String curp) {
        String curpNorm = curp.trim().toUpperCase();
        log.info("Consultando cliente por CURP");
        Cliente cliente = clienteRepository.findByCurp(curpNorm)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con la CURP indicada"));
        return clienteMapper.toClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorRfc(String rfc) {
        String rfcNorm = rfc.trim().toUpperCase();
        log.info("Consultando cliente por RFC");
        Cliente cliente = clienteRepository.findByRfc(rfcNorm)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con el RFC indicado"));
        return clienteMapper.toClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCorreo(String correo) {
        String correoNorm = correo.trim().toLowerCase();
        log.info("Consultando cliente por correo");
        Cliente cliente = clienteRepository.findByCorreo(correoNorm)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con el correo indicado"));
        return clienteMapper.toClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cliente por número de cuenta: {}", numeroCuenta);
        Cliente cliente = clienteRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente asociado al número de cuenta indicado"));
        return clienteMapper.toClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerClientesActivos() {
        log.info("Consultando todos los clientes activos");
        return clienteMapper.toClienteResponseList(clienteRepository.findByActivoTrueWithDomicilio());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerClientesPorRangoFechas(LocalDate desde, LocalDate hasta) {
        log.info("Consultando clientes por rango de fechas: {} a {}", desde, hasta);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ErrorValidacionException("La fecha inicial no puede ser posterior a la fecha final");
        }
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime fin = hasta != null ? hasta.atTime(LocalTime.MAX) : LocalDateTime.now();
        List<Cliente> clientes = clienteRepository.findByFechaRegistroBetweenWithDomicilio(inicio, fin);
        return clienteMapper.toClienteResponseList(clientes);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Integer id, ClienteUpdateRequest request) {
        log.info("Iniciando actualización para cliente ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            log.warn("Intento de actualización en cliente inactivo con ID: {}", id);
            throw new ClienteInactivoException("El cliente con ID " + id + " se encuentra inactivo y no puede ser actualizado");
        }

        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        if (!cliente.getCorreo().equalsIgnoreCase(nuevoCorreo)) {
            if (clienteRepository.existsByCorreo(nuevoCorreo) || usuarioRepository.existsByCorreo(nuevoCorreo)) {
                throw new CorreoDuplicadoException(nuevoCorreo);
            }
            cliente.setCorreo(nuevoCorreo);
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setCorreo(nuevoCorreo);
            }
        }

        cliente.setPrimerNombre(request.getPrimerNombre().trim());
        cliente.setSegundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null);
        cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().trim().isEmpty()
                ? request.getTelefonoAlternativo().trim() : null);
        cliente.setSexo(Sexo.fromCodigo(request.getSexo()));
        cliente.setEstadoCivil(EstadoCivil.fromCodigo(request.getEstadoCivil()));
        cliente.setNacionalidad(request.getNacionalidad().trim());
        cliente.setOcupacion(request.getOcupacion().trim());
        cliente.setEmpresa(request.getEmpresa().trim());
        cliente.setIngresoMensual(request.getIngresoMensual());

        DomicilioDto domDto = request.getDomicilio();
        if (cliente.getDomicilio() == null) {
            Domicilio dom = Domicilio.builder()
                    .cliente(cliente)
                    .build();
            cliente.setDomicilio(dom);
        }
        Domicilio dom = cliente.getDomicilio();
        dom.setCalle(domDto.getCalle().trim());
        dom.setNumeroExterior(domDto.getNumeroExterior().trim());
        dom.setNumeroInterior(domDto.getNumeroInterior() != null ? domDto.getNumeroInterior().trim() : null);
        dom.setColonia(domDto.getColonia().trim());
        dom.setMunicipio(domDto.getMunicipio().trim());
        dom.setEstado(domDto.getEstado().trim());
        dom.setPais(domDto.getPais().trim());
        dom.setCodigoPostal(domDto.getCodigoPostal().trim());

        Cliente actualizado = clienteRepository.save(cliente);
        log.info("Cliente ID: {} actualizado exitosamente", id);
        return clienteMapper.toClienteResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminarCliente(Integer id) {
        log.info("Iniciando baja lógica de cliente con ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            log.info("El cliente con ID {} ya se encontraba inactivo (baja lógica idempotente)", id);
            return;
        }

        cliente.setActivo(false);
        cliente.setFechaBaja(LocalDateTime.now());

        if (cliente.getCuentas() != null) {
            for (Cuenta cuenta : cliente.getCuentas()) {
                cuenta.setEstatus(EstatusCuenta.INACTIVA);
            }
        }

        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(false);
        }

        clienteRepository.save(cliente);
        log.info("Baja lógica completada para cliente ID: {}", id);
    }

    private String generarNumeroCuentaUnico() {
        int intentos = 0;
        while (intentos < 5) {
            long num = (long) (RANDOM.nextDouble() * 9000000000L) + 1000000000L;
            String numeroCuenta = String.valueOf(num);
            if (!cuentaRepository.existsByNumeroCuenta(numeroCuenta)) {
                return numeroCuenta;
            }
            intentos++;
        }
        throw new NegocioException("No se pudo generar un número de cuenta único tras varios intentos");
    }
}
