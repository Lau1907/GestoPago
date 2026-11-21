import '../../config/app_config.dart';
import '../../core/api/api_client.dart';
import '../../core/utils/id_generator.dart';
import '../../models/gestopago/pago_request.dart';
import '../../models/gestopago/pago_response.dart';

class PagoService {
  final ApiClient _apiClient;

  PagoService({ApiClient? apiClient}) : _apiClient = apiClient ?? ApiClient();

  /// Procesa el pago de servicio debitando de la cuenta del cliente y enviando a GestoPago.
  /// Genera una clave de idempotencia única por cada intento de pago.
  Future<PagoServicioResponse> pagarServicio({
    required int clienteId,
    required String numeroCuenta,
    required int idProducto,
    required double monto,
    required String referencia,
    String? digitoVerificador,
    String? customIdempotencyKey,
  }) async {
    final idempotencyKey = customIdempotencyKey ?? IdGenerator.generateIdempotencyKey();

    final request = PagoServicioRequest(
      clienteId: clienteId,
      numeroCuenta: numeroCuenta,
      idProducto: idProducto,
      monto: monto,
      referencia: referencia,
      digitoVerificador: digitoVerificador,
      idempotencyKey: idempotencyKey,
    );

    final responseJson = await _apiClient.post(
      AppConfig.pagarServicioEndpoint,
      body: request.toJson(),
      requiresAuth: true,
    );

    if (responseJson is Map<String, dynamic>) {
      return PagoServicioResponse.fromJson(responseJson);
    }

    throw Exception('Respuesta inesperada al procesar el pago');
  }

  /// Consulta el estado de una transacción mediante su clave de idempotencia
  Future<PagoServicioResponse> consultarTransaccion(String idempotencyKey) async {
    final responseJson = await _apiClient.post(
      AppConfig.consultarPagoEndpoint,
      body: {'idempotencyKey': idempotencyKey},
      requiresAuth: true,
    );

    if (responseJson is Map<String, dynamic>) {
      return PagoServicioResponse.fromJson(responseJson);
    }

    throw Exception('Error al consultar estado de transacción');
  }
}
