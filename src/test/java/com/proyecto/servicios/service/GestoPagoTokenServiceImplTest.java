package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceImplTest {

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    @InjectMocks
    private GestoPagoTokenServiceImpl gestoPagoTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gestoPagoTokenService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gestoPagoTokenService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(gestoPagoTokenService, "password", "password_prueba");
    }

    @Test
    @DisplayName("No debe guardar ni actualizar token cuando la respuesta de autenticación contiene token null")
    void renovarToken_TokenNull_NoGuardaEntity() {
        // Arrange
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken(null);
        response.setStatus(401);
        response.setMessage("Credenciales inválidas");

        when(gestoPagoAuthClient.authenticate(eq(83), eq("GPS83-TPV-17"), eq("password_prueba")))
                .thenReturn(response);

        // Act
        gestoPagoTokenService.renovarToken();

        // Assert
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("No debe guardar ni actualizar token cuando la respuesta de autenticación contiene token en blanco")
    void renovarToken_TokenEnBlanco_NoGuardaEntity() {
        // Arrange
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("   ");
        response.setStatus(401);

        when(gestoPagoAuthClient.authenticate(eq(83), eq("GPS83-TPV-17"), eq("password_prueba")))
                .thenReturn(response);

        // Act
        gestoPagoTokenService.renovarToken();

        // Assert
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe guardar exitosamente el token cuando la respuesta contiene un token válido")
    void renovarToken_TokenValido_GuardaEntity() {
        // Arrange
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("token_valido_12345");
        response.setStatus(200);

        GestoPagoToken tokenEntity = new GestoPagoToken();
        tokenEntity.setToken("token_valido_12345");

        when(gestoPagoAuthClient.authenticate(eq(83), eq("GPS83-TPV-17"), eq("password_prueba")))
                .thenReturn(response);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(eq(83), eq("GPS83-TPV-17")))
                .thenReturn(Optional.empty());
        when(tokenMapper.toEntity(response)).thenReturn(tokenEntity);

        // Act
        gestoPagoTokenService.renovarToken();

        // Assert
        verify(tokenRepository, times(1)).save(tokenEntity);
    }

    @Test
    @DisplayName("Debe obtener token activo desde el repositorio")
    void obtenerTokenActivo_Exitoso() {
        // Arrange
        GestoPagoToken tokenEntity = new GestoPagoToken();
        tokenEntity.setToken("token_valido_12345");

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(eq(83), eq("GPS83-TPV-17")))
                .thenReturn(Optional.of(tokenEntity));

        // Act
        Optional<GestoPagoToken> resultado = gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals("token_valido_12345", resultado.get().getToken());
    }
}
