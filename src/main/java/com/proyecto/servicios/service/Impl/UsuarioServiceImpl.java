package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.PasswordChangeRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Integer id, String authenticatedUserId) {
        log.info("Consultando usuario por ID: {}", id);
        validarPropioUsuario(id, authenticatedUserId);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
        return clienteMapper.toUsuarioResponse(usuario);
    }

    @Override
    @Transactional
    public void cambiarPassword(Integer id, PasswordChangeRequest request, String authenticatedUserId) {
        log.info("Solicitud de cambio de contraseña para usuario ID: {}", id);
        validarPropioUsuario(id, authenticatedUserId);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            log.warn("Fallo de cambio de contraseña: la contraseña actual es incorrecta");
            throw new ContrasenaInvalidaException("La contraseña actual proporcionada es incorrecta");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);
        log.info("Contraseña actualizada exitosamente para usuario ID: {}", id);
    }

    private void validarPropioUsuario(Integer targetUserId, String authenticatedUserId) {
        if (authenticatedUserId == null || !authenticatedUserId.equals(String.valueOf(targetUserId))) {
            log.warn("Acceso denegado: usuario autenticado {} intentó acceder/modificar usuario {}", authenticatedUserId, targetUserId);
            throw new NegocioException("Acceso denegado: no tiene permisos para consultar o modificar la cuenta de otro usuario", HttpStatus.FORBIDDEN);
        }
    }
}
