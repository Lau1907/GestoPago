package com.proyecto.servicios.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.ClienteCreateRequest;
import com.proyecto.servicios.model.onboarding.ClienteIdRequest;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.Verificacion2faRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CodigoAutenticacionRepository;
import com.proyecto.servicios.repositorys.onboarding.SesionUsuarioRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Autenticacion2faYSesionUnicaIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private GestoPagoTokenService gestoPagoTokenService;

        @Autowired
        private ClienteRepository clienteRepository;

        @Autowired
        private UsuarioRepository usuarioRepository;

        @Autowired
        private CodigoAutenticacionRepository codigoAutenticacionRepository;

        @Autowired
        private SesionUsuarioRepository sesionUsuarioRepository;

        @BeforeEach
        @AfterEach
        void limpiarDatos() {
                usuarioRepository.findByCorreo("alumno.2fa.test@unidep.mx").ifPresent(usuario -> {
                        sesionUsuarioRepository.findAll().stream()
                                        .filter(s -> s.getUsuario() != null
                                                        && usuario.getId().equals(s.getUsuario().getId()))
                                        .forEach(sesionUsuarioRepository::delete);
                        codigoAutenticacionRepository.findAll().stream()
                                        .filter(c -> c.getUsuario() != null
                                                        && usuario.getId().equals(c.getUsuario().getId()))
                                        .forEach(codigoAutenticacionRepository::delete);
                });
                clienteRepository.findByCurp("SALC950315HDFRPR99").ifPresent(clienteRepository::delete);
                clienteRepository.findByCorreo("alumno.2fa.test@unidep.mx").ifPresent(clienteRepository::delete);
                clienteRepository.findByRfc("SALC950315AA9").ifPresent(clienteRepository::delete);
        }

        @Test
        @DisplayName("Flujo completo Unidad II: Login con 2FA -> Validación OTP -> Token Final -> Control de Sesión Única (Web vs Mobile)")
        void flujoCompleto2faYControlSesionUnica() throws Exception {
                // =====================================================================
                // 1. Registrar Cliente y Usuario de Prueba
                // =====================================================================
                String correo = "alumno.2fa.test@unidep.mx";
                String password = "Password#2026";
                String curp = "SALC950315HDFRPR99";
                String rfc = "SALC950315AA9";

                DomicilioDto domicilio = DomicilioDto.builder()
                                .calle("Av. Reforma")
                                .numeroExterior("100")
                                .colonia("Juarez")
                                .municipio("Cuauhtemoc")
                                .estado("Ciudad de Mexico")
                                .pais("Mexico")
                                .codigoPostal("06600")
                                .build();

                ClienteCreateRequest clienteRequest = ClienteCreateRequest.builder()
                                .primerNombre("Carlos")
                                .apellidoPaterno("Sanchez")
                                .apellidoMaterno("Lopez")
                                .curp(curp)
                                .rfc(rfc)
                                .correo(correo)
                                .fechaNacimiento(LocalDate.of(1995, 3, 15))
                                .telefonoMovil("5512345679")
                                .sexo("H")
                                .estadoCivil("S")
                                .nacionalidad("Mexicana")
                                .ocupacion("Ingeniero")
                                .empresa("Tech Corp")
                                .ingresoMensual(new BigDecimal("35000.00"))
                                .domicilio(domicilio)
                                .password(password)
                                .build();

                MvcResult regResult = mockMvc.perform(post("/clientes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clienteRequest)))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode regJson = objectMapper.readTree(regResult.getResponse().getContentAsString());
                int clienteId = regJson.get("id").asInt();

                // =====================================================================
                // 2. Paso 1 de Login: Solicitud desde plataforma "WEB"
                // Debe detectar requerimiento de 2FA y generar código OTP de 6 dígitos
                // =====================================================================
                LoginRequest loginWebRequest = LoginRequest.builder()
                                .correo(correo)
                                .password(password)
                                .dispositivo("WEB")
                                .build();

                MvcResult loginWebResult = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginWebRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.requiere2FA").value(true))
                                .andExpect(jsonPath("$.tipo").value("PRE_AUTH"))
                                .andExpect(jsonPath("$.tokenTemporal").exists())
                                .andExpect(jsonPath("$.dispositivo").value("WEB"))
                                .andReturn();

                JsonNode loginWebJson = objectMapper.readTree(loginWebResult.getResponse().getContentAsString());
                String preAuthToken = loginWebJson.get("tokenTemporal").asText();

                Usuario usuarioTest = usuarioRepository.findByCorreo(correo).orElseThrow();
                String codigoOtpWeb = codigoAutenticacionRepository
                                .findTopByUsuarioIdAndUtilizadoFalseOrderByFechaCreacionDesc(usuarioTest.getId())
                                .orElseThrow()
                                .getCodigo();

                // Comprobar que el código OTP tiene exactamente 6 dígitos numéricos
                assertEquals(6, codigoOtpWeb.length());
                assertTrue(codigoOtpWeb.matches("^\\d{6}$"));

                ClienteIdRequest idReq = ClienteIdRequest.builder().id(clienteId).build();

                // =====================================================================
                // 3. Verificar que el token temporal PRE_AUTH NO otorga acceso a recursos
                // protegidos
                // =====================================================================
                mockMvc.perform(post("/clientes/consultar")
                                .header("Authorization", "Bearer " + preAuthToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(idReq)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.mensaje").value(
                                                "Token de pre-autenticación. Se requiere completar la validación de segundo factor (2FA)."));

                // =====================================================================
                // 4. Validar código 2FA incorrecto -> debe fallar con 401
                // =====================================================================
                Verificacion2faRequest wrongOtpReq = Verificacion2faRequest.builder()
                                .correo(correo)
                                .codigo("000000") // Código erróneo
                                .dispositivo("WEB")
                                .build();

                mockMvc.perform(post("/auth/2fa/verificar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongOtpReq)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.codigo").value(401));

                // =====================================================================
                // 5. Paso 2 de Login: Validar código 2FA correcto para plataforma "WEB"
                // Debe emitir el token final Bearer y registrar la sesión activa WEB
                // =====================================================================
                Verificacion2faRequest validOtpWebReq = Verificacion2faRequest.builder()
                                .correo(correo)
                                .codigo(codigoOtpWeb)
                                .dispositivo("WEB")
                                .build();

                MvcResult verifyWebResult = mockMvc.perform(post("/auth/2fa/verificar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOtpWebReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").exists())
                                .andExpect(jsonPath("$.tipo").value("Bearer"))
                                .andExpect(jsonPath("$.requiere2FA").value(false))
                                .andExpect(jsonPath("$.dispositivo").value("WEB"))
                                .andReturn();

                JsonNode verifyWebJson = objectMapper.readTree(verifyWebResult.getResponse().getContentAsString());
                String tokenJwtWeb = verifyWebJson.get("token").asText();

                // =====================================================================
                // 6. Consultar recurso protegido con el Token JWT final de "WEB" -> 200 OK
                // =====================================================================
                mockMvc.perform(post("/clientes/consultar")
                                .header("Authorization", "Bearer " + tokenJwtWeb)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(idReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.primerNombre").value("Carlos"));

                // =====================================================================
                // 7. CONTROL DE SESIÓN ÚNICA:
                // El usuario ahora inicia sesión desde la plataforma "MOBILE"
                // =====================================================================
                LoginRequest loginMobileRequest = LoginRequest.builder()
                                .correo(correo)
                                .password(password)
                                .dispositivo("MOBILE")
                                .build();

                MvcResult loginMobileResult = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginMobileRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.requiere2FA").value(true))
                                .andExpect(jsonPath("$.dispositivo").value("MOBILE"))
                                .andReturn();

                String codigoOtpMobile = codigoAutenticacionRepository
                                .findTopByUsuarioIdAndUtilizadoFalseOrderByFechaCreacionDesc(usuarioTest.getId())
                                .orElseThrow()
                                .getCodigo();

                // Validar 2FA de la sesión MOBILE
                Verificacion2faRequest validOtpMobileReq = Verificacion2faRequest.builder()
                                .correo(correo)
                                .codigo(codigoOtpMobile)
                                .dispositivo("MOBILE")
                                .build();

                MvcResult verifyMobileResult = mockMvc.perform(post("/auth/verificar-2fa") // probando el endpoint alias
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOtpMobileReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").exists())
                                .andExpect(jsonPath("$.dispositivo").value("MOBILE"))
                                .andReturn();

                JsonNode verifyMobileJson = objectMapper
                                .readTree(verifyMobileResult.getResponse().getContentAsString());
                String tokenJwtMobile = verifyMobileJson.get("token").asText();

                // =====================================================================
                // 8. Consultar con el nuevo token MOBILE -> 200 OK (sesión activa permitida)
                // =====================================================================
                mockMvc.perform(post("/clientes/consultar")
                                .header("Authorization", "Bearer " + tokenJwtMobile)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(idReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.primerNombre").value("Carlos"));

                // =====================================================================
                // 9. Intentar consultar con el token ANTERIOR de "WEB"
                // DEBE SER RECHAZADO (HTTP 401) porque la sesión WEB fue invalidada
                // al iniciarse la nueva sesión en MOBILE (Regla de Sesión Única).
                // =====================================================================
                mockMvc.perform(post("/clientes/consultar")
                                .header("Authorization", "Bearer " + tokenJwtWeb)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(idReq)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.codigo").value(401))
                                .andExpect(jsonPath("$.mensaje").value(
                                                "Sesión inválida o cerrada. Se ha iniciado sesión desde otro dispositivo o plataforma."));
        }
}
