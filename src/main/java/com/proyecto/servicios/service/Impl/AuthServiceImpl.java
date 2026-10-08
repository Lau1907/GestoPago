package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.security.JwtTokenProvider;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correoNorm = request.getCorreo().trim().toLowerCase();
        log.info("Intento de inicio de sesión");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correoNorm);
        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), usuarioOpt.get().getPasswordHash())) {
            log.warn("Fallo de autenticación: credenciales inválidas");
            throw new CredencialesInvalidasException("Credenciales inválidas");
        }

        Usuario usuario = usuarioOpt.get();
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn("Fallo de autenticación: usuario inactivo con ID {}", usuario.getId());
            throw new UsuarioInactivoException("El usuario se encuentra inactivo");
        }

        String token = jwtTokenProvider.generateToken(usuario.getId(), usuario.getCorreo());
        log.info("Inicio de sesión exitoso para usuario ID: {}", usuario.getId());

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .expiraEn(jwtTokenProvider.getExpirationMs())
                .build();
    }
}
