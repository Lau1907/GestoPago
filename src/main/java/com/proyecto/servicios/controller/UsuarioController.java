package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.PasswordChangeRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Integer id, Principal principal) {
        String authUserId = principal != null ? principal.getName() : null;
        UsuarioResponse response = usuarioService.obtenerPorId(id, authUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> cambiarPassword(@PathVariable Integer id,
                                                @Valid @RequestBody PasswordChangeRequest request,
                                                Principal principal) {
        String authUserId = principal != null ? principal.getName() : null;
        usuarioService.cambiarPassword(id, request, authUserId);
        return ResponseEntity.noContent().build();
    }
}
