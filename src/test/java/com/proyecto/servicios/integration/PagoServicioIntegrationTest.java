package com.proyecto.servicios.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoPaymentClient;
import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.onboarding.*;
import com.proyecto.servicios.model.gestopago.PagoConsultaRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.repositorys.gestopago.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PagoServicioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private TransaccionPagoRepository transaccionPagoRepository;

    @MockBean
    private GestoPagoTokenService gestoPagoTokenService;

    @MockBean
    private GestoPagoProductClient gestoPagoProductClient;

    @MockBean
    private GestoPagoPaymentClient gestoPagoPaymentClient;

    private Cliente clienteTest;
    private Cuenta cuentaTest;

    @BeforeEach
    void setUp() {
        GestoPagoToken tokenDummy = new GestoPagoToken();
        tokenDummy.setId(1);
        tokenDummy.setIdDistribuidor(83);
        tokenDummy.setCodigoDispositivo("GPS83-TPV-17");
        tokenDummy.setToken("mock_bearer_token");
        tokenDummy.setActivo(true);

        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(tokenDummy));

        String catalogoXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>Operacion realizada con exito</TEXTO>
                    </MENSAJE>
                    <PRODUCTOS>
                        <producto servicio="AGUAKAN (Cancun)" producto="Agua Cancun" idServicio="56" idProducto="185" idCatTipoServicio="15" tipoFront="2" hasDigitoVerificador="false" precio="10.0" showAyuda="false" tipoReferencia="c">
                            <legend><![CDATA[Leyenda de comprobante AGUAKAN]]></legend>
                        </producto>
                    </PRODUCTOS>
                </RESPONSE>
                """;
        when(gestoPagoProductClient.getProductList(anyString())).thenReturn(catalogoXml);

        // Crear cliente y cuenta de prueba con saldo de 200.00
        clienteTest = clienteRepository.findByCurp("HEGG560427MVZRRL99").orElseGet(() -> {
            Cliente c = Cliente.builder()
                    .primerNombre("Mariana")
                    .apellidoPaterno("Solis")
                    .apellidoMaterno("Vargas")
                    .curp("HEGG560427MVZRRL99")
                    .rfc("HEGG560427XYZ")
                    .correo("mariana.solis@example.com")
                    .fechaNacimiento(LocalDate.of(1995, 3, 20))
                    .telefonoMovil("9983334455")
                    .sexo(Sexo.MUJER)
                    .estadoCivil(EstadoCivil.SOLTERO)
                    .nacionalidad("Mexicana")
                    .ocupacion("Diseñadora")
                    .empresa("Studio Creativo")
                    .ingresoMensual(new BigDecimal("30000.00"))
                    .activo(true)
                    .build();
            return clienteRepository.save(c);
        });

        cuentaTest = cuentaRepository.findByNumeroCuenta("9988776655").orElseGet(() -> {
            Cuenta cu = Cuenta.builder()
                    .cliente(clienteTest)
                    .numeroCuenta("9988776655")
                    .saldo(new BigDecimal("200.00"))
                    .estatus(EstatusCuenta.ACTIVA)
                    .build();
            return cuentaRepository.save(cu);
        });

        // Asegurar saldo 200.00 al inicio de cada test
        cuentaTest.setSaldo(new BigDecimal("200.00"));
        cuentaRepository.save(cuentaTest);
    }

    @Test
    @DisplayName("Flujo End-to-End: Pago de servicio exitoso con débito real en BD H2 e idempotencia")
    void flujoCompleto_PagoServicioExitoso_EIdempotente() throws Exception {
        String xmlTxExitosa = """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>Operacion realizada con exito</TEXTO>
                    </MENSAJE>
                    <TRANSACCION>
                        <ID_TRANSACCION>789456</ID_TRANSACCION>
                        <FOLIO>GP-IT-20261027-001</FOLIO>
                        <AUTORIZACION>AUTH-INT-99</AUTORIZACION>
                        <FECHA>2026-10-27 17:15:00</FECHA>
                        <MONTO>10.00</MONTO>
                        <TICKET>Comprobante de Pago AGUAKAN</TICKET>
                    </TRANSACCION>
                </RESPONSE>
                """;

        when(gestoPagoPaymentClient.sendTx(anyString(), eq(185), eq("10.00"), eq("CONTRATO-1234"), any(), any(), any()))
                .thenReturn(xmlTxExitosa);

        PagoServicioRequest request = PagoServicioRequest.builder()
                .clienteId(clienteTest.getId())
                .numeroCuenta(cuentaTest.getNumeroCuenta())
                .idProducto(185)
                .monto(new BigDecimal("10.00"))
                .referencia("CONTRATO-1234")
                .idempotencyKey("IDEMP-INTEGRATION-001")
                .build();

        // Primer intento: Debe procesar el pago y debitar los $10.00 de la cuenta
        mockMvc.perform(post("/pagos/servicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EXITOSA"))
                .andExpect(jsonPath("$.folioGestoPago").value("GP-IT-20261027-001"))
                .andExpect(jsonPath("$.saldoAnterior").value(200.00))
                .andExpect(jsonPath("$.saldoNuevo").value(190.00));

        // Verificar el saldo actualizado directamente en la base de datos
        Cuenta cuentaActualizada = cuentaRepository.findByNumeroCuenta(cuentaTest.getNumeroCuenta()).orElseThrow();
        assertEquals(0, new BigDecimal("190.00").compareTo(cuentaActualizada.getSaldo()));

        // Segundo intento con la MISMA clave de idempotencia: Debe retornar la misma respuesta sin debitar nuevamente
        mockMvc.perform(post("/pagos/servicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EXITOSA"))
                .andExpect(jsonPath("$.saldoNuevo").value(190.00));

        cuentaActualizada = cuentaRepository.findByNumeroCuenta(cuentaTest.getNumeroCuenta()).orElseThrow();
        assertEquals(0, new BigDecimal("190.00").compareTo(cuentaActualizada.getSaldo()));

        // Consulta de la transacción
        PagoConsultaRequest consultaRequest = PagoConsultaRequest.builder()
                .idempotencyKey("IDEMP-INTEGRATION-001")
                .build();

        mockMvc.perform(post("/pagos/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consultaRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("IDEMP-INTEGRATION-001"))
                .andExpect(jsonPath("$.estado").value("EXITOSA"))
                .andExpect(jsonPath("$.folioGestoPago").value("GP-IT-20261027-001"));
    }

    @Test
    @DisplayName("Validación de Negocio: Saldo insuficiente debe retornar HTTP 400 Bad Request y no debitar saldo")
    void pagoServicio_SaldoInsuficiente_Retorna400() throws Exception {
        PagoServicioRequest requestMontoExcedido = PagoServicioRequest.builder()
                .clienteId(clienteTest.getId())
                .numeroCuenta(cuentaTest.getNumeroCuenta())
                .idProducto(185)
                .monto(new BigDecimal("500.00")) // Saldo actual es 200.00
                .referencia("CONTRATO-EXC")
                .idempotencyKey("IDEMP-INTEGRATION-EXCEDIDO")
                .build();

        mockMvc.perform(post("/pagos/servicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMontoExcedido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));

        // El saldo en base de datos debe permanecer inalterado
        Cuenta cuentaActual = cuentaRepository.findByNumeroCuenta(cuentaTest.getNumeroCuenta()).orElseThrow();
        assertEquals(0, new BigDecimal("200.00").compareTo(cuentaActual.getSaldo()));
    }
}
