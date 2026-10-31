package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoPaymentClient;
import com.proyecto.servicios.entity.gestopago.EstadoTransaccion;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.TransaccionPago;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.EstatusCuenta;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.gestopago.*;
import com.proyecto.servicios.repositorys.gestopago.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private GestoPagoProductService gestoPagoProductService;

    @Mock
    private GestoPagoPaymentClient gestoPagoPaymentClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    private Cliente clienteDummy;
    private Cuenta cuentaDummy;
    private GestoPagoToken tokenDummy;
    private GestoPagoProductListResponse catalogoDummy;
    private PagoServicioRequest requestValido;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoServicioService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(pagoServicioService, "codigoDispositivo", "GPS83-TPV-17");

        clienteDummy = Cliente.builder()
                .id(1)
                .primerNombre("Juan")
                .apellidoPaterno("Perez")
                .activo(true)
                .build();

        cuentaDummy = Cuenta.builder()
                .id(10)
                .cliente(clienteDummy)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("1000.00"))
                .estatus(EstatusCuenta.ACTIVA)
                .build();

        tokenDummy = new GestoPagoToken();
        tokenDummy.setId(1);
        tokenDummy.setIdDistribuidor(83);
        tokenDummy.setCodigoDispositivo("GPS83-TPV-17");
        tokenDummy.setToken("token_valido_gestopago");
        tokenDummy.setActivo(true);

        GestoPagoProductoItem itemAgua = GestoPagoProductoItem.builder()
                .idProducto(185)
                .idServicio(56)
                .producto("Agua Cancun")
                .servicio("AGUAKAN (Cancun)")
                .precio("10.0")
                .tipoFront(2)
                .hasDigitoVerificador(false)
                .build();

        GestoPagoProductoItem itemTelmex = GestoPagoProductoItem.builder()
                .idProducto(200)
                .idServicio(60)
                .producto("Telmex")
                .servicio("Telefonia Fija")
                .precio("0.0")
                .tipoFront(1)
                .hasDigitoVerificador(true)
                .build();

        catalogoDummy = GestoPagoProductListResponse.builder()
                .mensaje(GestoPagoMensajeResponse.builder().codigo("01").texto("OK").build())
                .productos(List.of(itemAgua, itemTelmex))
                .build();

        requestValido = PagoServicioRequest.builder()
                .clienteId(1)
                .numeroCuenta("1234567890")
                .idProducto(185)
                .monto(new BigDecimal("10.00"))
                .referencia("9981234567")
                .idempotencyKey("TX-20261027-001")
                .build();
    }

    @Test
    @DisplayName("Debe procesar el pago exitosamente y debitar el saldo de la cuenta")
    void pagarServicio_Exitoso() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey("TX-20261027-001")).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));
        when(gestoPagoProductService.obtenerProductos()).thenReturn(catalogoDummy);
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenDummy));

        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(invocation -> {
            TransaccionPago t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(101);
            return t;
        });

        String xmlExitoso = """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>Operacion realizada con exito</TEXTO>
                    </MENSAJE>
                    <TRANSACCION>
                        <ID_TRANSACCION>999111</ID_TRANSACCION>
                        <FOLIO>GP-FOLIO-123</FOLIO>
                        <AUTORIZACION>AUTH-456</AUTORIZACION>
                        <FECHA>2026-10-27 17:00:00</FECHA>
                        <MONTO>10.00</MONTO>
                        <TICKET>Ticket comprobante de pago AGUAKAN</TICKET>
                    </TRANSACCION>
                </RESPONSE>
                """;

        when(gestoPagoPaymentClient.sendTx(
                eq("Bearer token_valido_gestopago"), eq(185), eq("10.00"), eq("9981234567"), isNull(), eq(2), eq(56)))
                .thenReturn(xmlExitoso);

        // Act
        PagoServicioResponse response = pagoServicioService.pagarServicio(requestValido);

        // Assert
        assertNotNull(response);
        assertEquals("EXITOSA", response.getEstado());
        assertEquals("01", response.getCodigoGestoPago());
        assertEquals("GP-FOLIO-123", response.getFolioGestoPago());
        assertEquals("AUTH-456", response.getAutorizacionGestoPago());
        assertEquals(new BigDecimal("1000.00"), response.getSaldoAnterior());
        assertEquals(new BigDecimal("990.00"), response.getSaldoNuevo());
        assertEquals(new BigDecimal("990.00"), cuentaDummy.getSaldo());

        verify(cuentaRepository, times(1)).save(cuentaDummy);
        verify(transaccionPagoRepository, times(2)).save(any(TransaccionPago.class));
    }

    @Test
    @DisplayName("Debe retornar respuesta idempotente sin volver a cobrar si la transacción ya fue exitosa")
    void pagarServicio_Idempotencia_TransaccionYaExitosa() {
        // Arrange
        TransaccionPago txExistente = TransaccionPago.builder()
                .id(101)
                .idempotencyKey("TX-20261027-001")
                .cliente(clienteDummy)
                .cuenta(cuentaDummy)
                .numeroCuenta("1234567890")
                .idProducto(185)
                .servicio("AGUAKAN (Cancun)")
                .producto("Agua Cancun")
                .monto(new BigDecimal("10.00"))
                .saldoAnterior(new BigDecimal("1000.00"))
                .saldoPosterior(new BigDecimal("990.00"))
                .referenciaServicio("9981234567")
                .estatus(EstadoTransaccion.EXITOSA)
                .codigoRespuestaGestoPago("01")
                .folioGestoPago("GP-FOLIO-123")
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(transaccionPagoRepository.findByIdempotencyKey("TX-20261027-001")).thenReturn(Optional.of(txExistente));

        // Act
        PagoServicioResponse response = pagoServicioService.pagarServicio(requestValido);

        // Assert
        assertNotNull(response);
        assertEquals("EXITOSA", response.getEstado());
        assertEquals("GP-FOLIO-123", response.getFolioGestoPago());
        assertEquals(new BigDecimal("990.00"), response.getSaldoNuevo());

        verifyNoInteractions(gestoPagoPaymentClient);
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar TransaccionDuplicadaException si la clave está en revisión por timeout previo")
    void pagarServicio_Idempotencia_TransaccionEnRevision() {
        // Arrange
        TransaccionPago txExistente = TransaccionPago.builder()
                .id(102)
                .idempotencyKey("TX-20261027-001")
                .estatus(EstadoTransaccion.TIMEOUT_REVISION)
                .build();

        when(transaccionPagoRepository.findByIdempotencyKey("TX-20261027-001")).thenReturn(Optional.of(txExistente));

        // Act & Assert
        assertThrows(TransaccionDuplicadaException.class, () -> pagoServicioService.pagarServicio(requestValido));
        verifyNoInteractions(gestoPagoPaymentClient);
    }

    @Test
    @DisplayName("Debe lanzar CuentaNoEncontradaException si la cuenta no existe en BD")
    void pagarServicio_CuentaNoEncontrada() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CuentaNoEncontradaException.class, () -> pagoServicioService.pagarServicio(requestValido));
    }

    @Test
    @DisplayName("Debe lanzar CuentaNoPerteneceAlClienteException si la cuenta pertenece a otro cliente")
    void pagarServicio_CuentaNoPerteneceAlCliente() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        Cliente otroCliente = Cliente.builder().id(99).primerNombre("Otro").activo(true).build();
        cuentaDummy.setCliente(otroCliente);
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));

        // Act & Assert
        assertThrows(CuentaNoPerteneceAlClienteException.class, () -> pagoServicioService.pagarServicio(requestValido));
    }

    @Test
    @DisplayName("Debe lanzar ClienteInactivoException si el cliente titular se encuentra inactivo")
    void pagarServicio_ClienteInactivo() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        clienteDummy.setActivo(false);
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));

        // Act & Assert
        assertThrows(ClienteInactivoException.class, () -> pagoServicioService.pagarServicio(requestValido));
    }

    @Test
    @DisplayName("Debe lanzar CuentaInactivaException si el estatus de la cuenta no es ACTIVA ('A')")
    void pagarServicio_CuentaInactiva() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        cuentaDummy.setEstatus(EstatusCuenta.INACTIVA);
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));

        // Act & Assert
        assertThrows(CuentaInactivaException.class, () -> pagoServicioService.pagarServicio(requestValido));
    }

    @Test
    @DisplayName("Debe lanzar SaldoInsuficienteException si el saldo de la cuenta es estrictamente menor al monto a pagar")
    void pagarServicio_SaldoInsuficiente() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        cuentaDummy.setSaldo(new BigDecimal("5.00")); // Monto requerido es 10.00
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));

        // Act & Assert
        SaldoInsuficienteException ex = assertThrows(SaldoInsuficienteException.class,
                () -> pagoServicioService.pagarServicio(requestValido));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
    }

    @Test
    @DisplayName("Debe lanzar ProductoNoEncontradoException si el ID de producto no existe en el catálogo")
    void pagarServicio_ProductoNoEncontrado() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));
        when(gestoPagoProductService.obtenerProductos()).thenReturn(catalogoDummy);

        requestValido.setIdProducto(99999);

        // Act & Assert
        assertThrows(ProductoNoEncontradoException.class, () -> pagoServicioService.pagarServicio(requestValido));
    }

    @Test
    @DisplayName("Debe lanzar MontoInvalidoProductoException si el monto no coincide con el precio fijo del catálogo")
    void pagarServicio_MontoInvalidoProductoFijo() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));
        when(gestoPagoProductService.obtenerProductos()).thenReturn(catalogoDummy);

        requestValido.setMonto(new BigDecimal("15.00")); // Precio en catálogo es 10.0

        // Act & Assert
        assertThrows(MontoInvalidoProductoException.class, () -> pagoServicioService.pagarServicio(requestValido));
    }

    @Test
    @DisplayName("Debe lanzar ErrorValidacionException si el producto requiere dígito verificador y no se proporciona")
    void pagarServicio_RequiereDigitoVerificador() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));
        when(gestoPagoProductService.obtenerProductos()).thenReturn(catalogoDummy);

        requestValido.setIdProducto(200); // Telmex con hasDigitoVerificador=true
        requestValido.setDigitoVerificador(null);

        // Act & Assert
        assertThrows(ErrorValidacionException.class, () -> pagoServicioService.pagarServicio(requestValido));
    }

    @Test
    @DisplayName("Debe manejar la incertidumbre por Timeout (HTTP 504) marcando la transacción en TIMEOUT_REVISION")
    void pagarServicio_TimeoutIncertidumbre_MarcaRevisionYNoDebitaSaldo() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));
        when(gestoPagoProductService.obtenerProductos()).thenReturn(catalogoDummy);
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenDummy));

        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Request request = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                new HashMap<>(), null, new RequestTemplate());
        RetryableException retryableException = new RetryableException(
                504, "Read timed out with GestoPago", Request.HttpMethod.POST, new Date(), request);

        when(gestoPagoPaymentClient.sendTx(anyString(), anyInt(), anyString(), anyString(), any(), any(), any()))
                .thenThrow(retryableException);

        // Act & Assert
        ExternalIntegrationException ex = assertThrows(ExternalIntegrationException.class,
                () -> pagoServicioService.pagarServicio(requestValido));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
        assertTrue(ex.getMessage().contains("Tiempo de espera agotado"));

        // Verificar que el saldo de la cuenta NO se debitó de forma errónea
        assertEquals(new BigDecimal("1000.00"), cuentaDummy.getSaldo());
        verify(cuentaRepository, never()).save(cuentaDummy);

        // Verificar que la transacción quedó guardada en TIMEOUT_REVISION
        verify(transaccionPagoRepository, atLeastOnce()).save(argThat(tx ->
                tx.getEstatus() == EstadoTransaccion.TIMEOUT_REVISION
        ));
    }

    @Test
    @DisplayName("Debe manejar el rechazo de GestoPago (código != 01) sin debitar saldo de la cuenta")
    void pagarServicio_RechazoGestoPago_MarcaFallidaYNoDebita() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));
        when(gestoPagoProductService.obtenerProductos()).thenReturn(catalogoDummy);
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenDummy));

        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String xmlRechazo = """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                    <MENSAJE>
                        <CODIGO>02</CODIGO>
                        <TEXTO>Referencia de servicio no valida o contrato vencido</TEXTO>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(gestoPagoPaymentClient.sendTx(anyString(), anyInt(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(xmlRechazo);

        // Act & Assert
        ExternalIntegrationException ex = assertThrows(ExternalIntegrationException.class,
                () -> pagoServicioService.pagarServicio(requestValido));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Referencia de servicio no valida"));

        // El saldo permanece intacto
        assertEquals(new BigDecimal("1000.00"), cuentaDummy.getSaldo());
        verify(cuentaRepository, never()).save(cuentaDummy);

        verify(transaccionPagoRepository, atLeastOnce()).save(argThat(tx ->
                tx.getEstatus() == EstadoTransaccion.FALLIDA && "02".equals(tx.getCodigoRespuestaGestoPago())
        ));
    }

    @Test
    @DisplayName("Debe consultar una transacción existente correctamente mediante Request Body")
    void consultarTransaccion_Exitoso() {
        // Arrange
        TransaccionPago txExistente = TransaccionPago.builder()
                .id(105)
                .idempotencyKey("TX-20261027-001")
                .cliente(clienteDummy)
                .cuenta(cuentaDummy)
                .numeroCuenta("1234567890")
                .idProducto(185)
                .servicio("AGUAKAN (Cancun)")
                .producto("Agua Cancun")
                .monto(new BigDecimal("10.00"))
                .saldoAnterior(new BigDecimal("1000.00"))
                .saldoPosterior(new BigDecimal("990.00"))
                .referenciaServicio("9981234567")
                .estatus(EstadoTransaccion.EXITOSA)
                .codigoRespuestaGestoPago("01")
                .folioGestoPago("GP-FOLIO-123")
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(transaccionPagoRepository.findByIdempotencyKey("TX-20261027-001")).thenReturn(Optional.of(txExistente));

        PagoConsultaRequest request = PagoConsultaRequest.builder()
                .idempotencyKey("TX-20261027-001")
                .build();

        // Act
        PagoServicioResponse response = pagoServicioService.consultarTransaccion(request);

        // Assert
        assertNotNull(response);
        assertEquals("TX-20261027-001", response.getIdempotencyKey());
        assertEquals("EXITOSA", response.getEstado());
    }

    @Test
    @DisplayName("Debe lanzar NegocioException NOT_FOUND si la transacción consultada no existe")
    void consultarTransaccion_NoEncontrada() {
        // Arrange
        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        PagoConsultaRequest request = PagoConsultaRequest.builder()
                .idempotencyKey("TX-INEXISTENTE")
                .build();

        // Act & Assert
        NegocioException ex = assertThrows(NegocioException.class,
                () -> pagoServicioService.consultarTransaccion(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Modo Mock: Debe procesar el pago exitosamente y debitar saldo simulando respuesta sendTx ante 401")
    void pagarServicio_ModoMockActivo_ProcesaPagoExitosamente() {
        // Arrange
        ReflectionTestUtils.setField(pagoServicioService, "mockEnabled", true);
        ReflectionTestUtils.setField(pagoServicioService, "password", "");
        ReflectionTestUtils.setField(pagoServicioService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(pagoServicioService, "codigoDispositivo", "GPS83-TPV-17");

        when(transaccionPagoRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuentaDummy));
        when(gestoPagoProductService.obtenerProductos()).thenReturn(catalogoDummy);
        when(gestoPagoTokenService.obtenerTokenActivo(eq(83), eq("GPS83-TPV-17"))).thenReturn(Optional.of(tokenDummy));
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        feign.Request feignRequest = feign.Request.create(feign.Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                new java.util.HashMap<>(), null, new feign.RequestTemplate());
        feign.FeignException.Unauthorized unauthorized = new feign.FeignException.Unauthorized("Unauthorized", feignRequest, null, new java.util.HashMap<>());
        when(gestoPagoPaymentClient.sendTx(any(), any(), any(), any(), any(), any(), any())).thenThrow(unauthorized);

        // Act
        PagoServicioResponse response = pagoServicioService.pagarServicio(requestValido);

        // Assert
        assertNotNull(response);
        assertEquals("EXITOSA", response.getEstado());
        assertEquals("01", response.getCodigoGestoPago());
        assertNotNull(response.getFolioGestoPago());
        assertTrue(response.getFolioGestoPago().startsWith("MOCK-"));
        assertEquals(new BigDecimal("990.00"), cuentaDummy.getSaldo());
        verify(cuentaRepository, times(1)).save(cuentaDummy);
        verify(gestoPagoPaymentClient, times(1)).sendTx(any(), any(), any(), any(), any(), any(), any());
    }
}
