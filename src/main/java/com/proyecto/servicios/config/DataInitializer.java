package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.onboarding.*;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Inicializador de datos predeterminados para la base de datos persistente H2.
 * Solo se ejecuta si la tabla de usuarios/clientes se encuentra vacía.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("Base de datos persistente ya contiene datos ({} usuarios registrados). Omitiendo DataInitializer.",
                    usuarioRepository.count());
            return;
        }

        log.info("Base de datos vacía. Precargando usuarios y cuentas de prueba para facilitar pruebas del ecosistema GestoPago...");

        // 1. Cliente 1: Alumno Seguridad (alumno.seguridad@unidep.mx / Password#2026)
        Cliente cliente1 = Cliente.builder()
                .primerNombre("Carlos")
                .apellidoPaterno("Sanchez")
                .apellidoMaterno("Lopez")
                .curp("SALC950315HDFRPR01")
                .rfc("SALC950315AA1")
                .correo("alumno.seguridad@unidep.mx")
                .fechaNacimiento(LocalDate.of(1995, 3, 15))
                .telefonoMovil("5512345678")
                .sexo(Sexo.HOMBRE)
                .estadoCivil(EstadoCivil.SOLTERO)
                .nacionalidad("Mexicana")
                .ocupacion("Ingeniero")
                .empresa("Tech Corp")
                .ingresoMensual(new BigDecimal("35000.00"))
                .activo(true)
                .cuentas(new ArrayList<>())
                .build();

        Domicilio domicilio1 = Domicilio.builder()
                .cliente(cliente1)
                .calle("Av. Reforma")
                .numeroExterior("100")
                .colonia("Juarez")
                .municipio("Cuauhtemoc")
                .estado("Ciudad de Mexico")
                .pais("Mexico")
                .codigoPostal("06600")
                .build();
        cliente1.setDomicilio(domicilio1);

        Cuenta cuenta1 = Cuenta.builder()
                .cliente(cliente1)
                .numeroCuenta("1000000001")
                .saldo(new BigDecimal("2500.00"))
                .estatus(EstatusCuenta.ACTIVA)
                .build();
        cliente1.getCuentas().add(cuenta1);

        Usuario usuario1 = Usuario.builder()
                .cliente(cliente1)
                .correo("alumno.seguridad@unidep.mx")
                .passwordHash(passwordEncoder.encode("Password#2026"))
                .activo(true)
                .dosFactoresActivo(true)
                .build();
        cliente1.setUsuario(usuario1);

        clienteRepository.save(cliente1);

        // 2. Cliente 2: Juan Perez (juan.perez@example.com / Password123!)
        Cliente cliente2 = Cliente.builder()
                .primerNombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Garcia")
                .curp("PEGJ850101HDFRPR01")
                .rfc("PEGJ850101AA1")
                .correo("juan.perez@example.com")
                .fechaNacimiento(LocalDate.of(1985, 1, 1))
                .telefonoMovil("5598765432")
                .sexo(Sexo.HOMBRE)
                .estadoCivil(EstadoCivil.CASADO)
                .nacionalidad("Mexicana")
                .ocupacion("Administrador")
                .empresa("Consultoria SC")
                .ingresoMensual(new BigDecimal("28000.00"))
                .activo(true)
                .cuentas(new ArrayList<>())
                .build();

        Domicilio domicilio2 = Domicilio.builder()
                .cliente(cliente2)
                .calle("Insurgentes Sur")
                .numeroExterior("500")
                .colonia("Del Valle")
                .municipio("Benito Juarez")
                .estado("Ciudad de Mexico")
                .pais("Mexico")
                .codigoPostal("03100")
                .build();
        cliente2.setDomicilio(domicilio2);

        Cuenta cuenta2 = Cuenta.builder()
                .cliente(cliente2)
                .numeroCuenta("1000000002")
                .saldo(new BigDecimal("1800.00"))
                .estatus(EstatusCuenta.ACTIVA)
                .build();
        cliente2.getCuentas().add(cuenta2);

        Usuario usuario2 = Usuario.builder()
                .cliente(cliente2)
                .correo("juan.perez@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .activo(true)
                .dosFactoresActivo(true)
                .build();
        cliente2.setUsuario(usuario2);

        clienteRepository.save(cliente2);

        log.info("Precarga exitosa de 2 clientes de prueba con saldo inicial:");
        log.info("1. alumno.seguridad@unidep.mx | Password#2026 | Cuenta: 1000000001 ($2,500.00)");
        log.info("2. juan.perez@example.com | Password123! | Cuenta: 1000000002 ($1,800.00)");
    }
}
