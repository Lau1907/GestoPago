package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.SesionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final SesionService sesionService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UsuarioRepository usuarioRepository,
                                   SesionService sesionService,
                                   JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarioRepository = usuarioRepository;
        this.sesionService = sesionService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UsuarioRepository usuarioRepository,
                                   SesionService sesionService) {
        this(jwtTokenProvider, usuarioRepository, sesionService, new JwtAuthenticationEntryPoint());
    }

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UsuarioRepository usuarioRepository) {
        this(jwtTokenProvider, usuarioRepository, null, new JwtAuthenticationEntryPoint());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // ── 1. CORS preflight: pasar sin validar token ──────────────────────────
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 2. Endpoints de autenticación pública: pasar libremente sin validar tokens residuales ──
        String path = request.getRequestURI();
        String servletPath = request.getServletPath() != null ? request.getServletPath() : path;
        if (path.startsWith("/auth/") || servletPath.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 3. Validación de token JWT y Control de Sesión Única ────────────────
        try {
            String token = getJwtFromRequest(request);
            if (StringUtils.hasText(token)) {
                // Verificar si es token de pre-autenticación (2FA pendiente)
                String scope = jwtTokenProvider.getScopeFromToken(token);
                if ("PRE_AUTH".equalsIgnoreCase(scope)) {
                    log.warn("Token de pre-autenticación no autorizado para acceder al recurso: {}", path);
                    request.setAttribute("SESSION_INVALIDA_MENSAJE",
                            "Token de pre-autenticación. Se requiere completar la validación de segundo factor (2FA).");
                    if (jwtAuthenticationEntryPoint != null) {
                        jwtAuthenticationEntryPoint.commence(request, response, null);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Pre-auth token no autorizado");
                    }
                    return;
                }

                Integer usuarioId = jwtTokenProvider.validateAndGetUsuarioId(token);
                if (usuarioId == null) {
                    log.warn("Token JWT inválido o expirado para recurso: {}", path);
                    request.setAttribute("SESSION_INVALIDA_MENSAJE",
                            "Token JWT inválido o expirado. Debe iniciar sesión nuevamente.");
                    if (jwtAuthenticationEntryPoint != null) {
                        jwtAuthenticationEntryPoint.commence(request, response, null);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                    }
                    return;
                }

                // Control de Sesión Única: Verificar si la sesión asociada al token sigue activa
                String sessionId = jwtTokenProvider.getSessionIdFromToken(token);
                if (sessionId != null && sesionService != null) {
                    boolean sesionActiva = sesionService.isSesionActiva(sessionId);
                    if (!sesionActiva) {
                        log.warn("CONTROL DE SESIÓN ÚNICA: Sesión {} invalidada o cerrada para usuario ID {}. Acceso denegado con HTTP 401 en {}.",
                                sessionId, usuarioId, path);
                        request.setAttribute("SESSION_INVALIDA_MENSAJE",
                                "Sesión inválida o cerrada. Se ha iniciado sesión desde otro dispositivo o plataforma.");
                        if (jwtAuthenticationEntryPoint != null) {
                            jwtAuthenticationEntryPoint.commence(request, response, null);
                        } else {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sesión inválida");
                        }
                        return;
                    }
                } else if (sesionService != null) {
                    // Token sin sessionId pero sesionService activo: verificar si el usuario tiene sesiones activas
                    log.warn("Token de usuario ID {} sin sessionId. Se deniega acceso por control de sesión única estricto.", usuarioId);
                    request.setAttribute("SESSION_INVALIDA_MENSAJE",
                            "Sesión inválida o cerrada. Se requiere iniciar sesión nuevamente.");
                    if (jwtAuthenticationEntryPoint != null) {
                        jwtAuthenticationEntryPoint.commence(request, response, null);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sesión requerida");
                    }
                    return;
                }

                // Verificación en BD en cada petición para validar que siga existiendo y activo
                Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
                if (usuarioOpt.isPresent() && Boolean.TRUE.equals(usuarioOpt.get().getActivo())) {
                    Usuario usuario = usuarioOpt.get();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    String.valueOf(usuario.getId()),
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Usuario con ID {} inactivo o eliminado. Token rechazado.", usuarioId);
                    request.setAttribute("SESSION_INVALIDA_MENSAJE", "Usuario inactivo o no encontrado.");
                    if (jwtAuthenticationEntryPoint != null) {
                        jwtAuthenticationEntryPoint.commence(request, response, null);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario inactivo");
                    }
                    return;
                }
            }
        } catch (Exception ex) {
            log.error("Error procesando autenticación JWT en filtro: {}", ex.getMessage());
            request.setAttribute("SESSION_INVALIDA_MENSAJE", "Error validando credenciales de sesión.");
            if (jwtAuthenticationEntryPoint != null) {
                jwtAuthenticationEntryPoint.commence(request, response, null);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error de autenticación");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
