import 'package:flutter/foundation.dart';
import '../../core/api/api_exceptions.dart';
import '../../core/utils/id_generator.dart';
import '../../models/gestopago/pago_response.dart';
import '../../services/historial_service.dart';
import '../../services/pago_service.dart';

/// Los 4 estados explícitos de compra en la aplicación móvil
enum EstadoPago {
  inicial,
  procesando,
  exitoso,           // Compra exitosa (HTTP 200, estado == EXITOSA)
  rechazado,         // Compra rechazada (fondos insuficientes, regla de negocio, estado == FALLIDA)
  pendienteRevision, // Compra pendiente (timeout externo, estado == TIMEOUT_REVISION / INICIADA)
  errorConexion,     // Error de comunicación (sin red, host inalcanzable o backend apagado)
}

class PagoProvider extends ChangeNotifier {
  final PagoService _pagoService;
  final HistorialService _historialService;

  EstadoPago _estado = EstadoPago.inicial;
  String? _errorMessage;
  PagoServicioResponse? _ultimoComprobante;
  String? _currentIdempotencyKey;

  EstadoPago get estado => _estado;
  bool get isProcesando => _estado == EstadoPago.procesando;
  String? get errorMessage => _errorMessage;
  PagoServicioResponse? get ultimoComprobante => _ultimoComprobante;
  String? get currentIdempotencyKey => _currentIdempotencyKey;

  PagoProvider({
    PagoService? pagoService,
    HistorialService? historialService,
  })  : _pagoService = pagoService ?? PagoService(),
        _historialService = historialService ?? HistorialService();

  /// Prepara e inicializa una nueva transacción generando una IdempotencyKey única
  void iniciarNuevaTransaccion() {
    _estado = EstadoPago.inicial;
    _errorMessage = null;
    _ultimoComprobante = null;
    _currentIdempotencyKey = IdGenerator.generateIdempotencyKey();
    notifyListeners();
  }

  /// Procesa la compra / pago de servicio hacia el backend de Spring Boot
  /// Maneja explícitamente los 4 estados de compra basados en la respuesta real del servidor.
  Future<PagoServicioResponse?> ejecutarPago({
    required int clienteId,
    required String numeroCuenta,
    required int idProducto,
    required double monto,
    required String referencia,
    String? digitoVerificador,
  }) async {
    _estado = EstadoPago.procesando;
    _errorMessage = null;
    notifyListeners();

    // Clave de idempotencia única por intento para evitar duplicados
    final idempotencyKey = _currentIdempotencyKey ?? IdGenerator.generateIdempotencyKey();
    _currentIdempotencyKey = idempotencyKey;

    try {
      final response = await _pagoService.pagarServicio(
        clienteId: clienteId,
        numeroCuenta: numeroCuenta,
        idProducto: idProducto,
        monto: monto,
        referencia: referencia,
        digitoVerificador: digitoVerificador,
        customIdempotencyKey: idempotencyKey,
      );

      _ultimoComprobante = response;

      // 1. COMPRA EXITOSA
      if (response.isExitosa) {
        _estado = EstadoPago.exitoso;
        await _historialService.registrarCompraExitosa(response);
      }
      // 2. COMPRA PENDIENTE (proveedor externo en revisión)
      else if (response.isPendiente) {
        _estado = EstadoPago.pendienteRevision;
        _errorMessage = response.mensajeGestoPago ??
            'La transacción se encuentra en estado de verificación con el proveedor externo.';
      }
      // 3. COMPRA RECHAZADA (GestoPago devolvió estatus FALLIDA)
      else {
        _estado = EstadoPago.rechazado;
        _errorMessage = response.mensajeGestoPago ??
            'La transacción fue rechazada por el proveedor del servicio.';
      }

      notifyListeners();
      return response;
    } on UnauthorizedSessionException catch (e) {
      // CONTROL DE SESIÓN ÚNICA: La sesión fue cerrada en otro dispositivo
      // Limpiar cualquier estado previo y re-lanzar para que la UI cierre el modal y no ofrezca reintentos
      _estado = EstadoPago.inicial;
      _errorMessage = e.message;
      _ultimoComprobante = null;
      notifyListeners();
      rethrow;
    } on ValidationException catch (e) {
      // 3. COMPRA RECHAZADA (HTTP 400/404/409/422: saldo insuficiente, cuenta no válida, etc.)
      _estado = EstadoPago.rechazado;
      _errorMessage = e.message;
      notifyListeners();
      return null;
    } on ServerException catch (e) {
      // Si el error del servidor es un timeout 504 o revisión de GestoPago -> COMPRA PENDIENTE
      if (e.isPendingReview) {
        _estado = EstadoPago.pendienteRevision;
        _errorMessage = e.message;
      } else {
        // Error 500 u otro fallo de servidor -> COMPRA RECHAZADA
        _estado = EstadoPago.rechazado;
        _errorMessage = e.message;
      }
      notifyListeners();
      return null;
    } on NetworkException catch (e) {
      // 4. ERROR DE COMUNICACIÓN (sin conexión, backend apagado o timeout de red)
      _estado = EstadoPago.errorConexion;
      _errorMessage = e.message;
      notifyListeners();
      return null;
    } catch (e) {
      // Fallback genérico para errores imprevistos
      _estado = EstadoPago.errorConexion;
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      notifyListeners();
      return null;
    }
  }

  /// Permite reconsultar una transacción pendiente de revisión con el backend
  Future<void> reconsultarTransaccion() async {
    if (_currentIdempotencyKey == null) return;

    _estado = EstadoPago.procesando;
    notifyListeners();

    try {
      final response = await _pagoService.consultarTransaccion(_currentIdempotencyKey!);
      _ultimoComprobante = response;

      if (response.isExitosa) {
        _estado = EstadoPago.exitoso;
        await _historialService.registrarCompraExitosa(response);
      } else if (response.isPendiente) {
        _estado = EstadoPago.pendienteRevision;
        _errorMessage = response.mensajeGestoPago ??
            'La transacción continúa en proceso de verificación por el proveedor.';
      } else {
        _estado = EstadoPago.rechazado;
        _errorMessage = response.mensajeGestoPago ??
            'La transacción fue rechazada en la revisión final.';
      }
      notifyListeners();
    } on NetworkException catch (e) {
      _estado = EstadoPago.errorConexion;
      _errorMessage = e.message;
      notifyListeners();
    } catch (e) {
      _estado = EstadoPago.errorConexion;
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      notifyListeners();
    }
  }
}
