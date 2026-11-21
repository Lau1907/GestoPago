import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../config/app_config.dart';
import '../core/api/api_client.dart';
import '../models/gestopago/pago_response.dart';

class HistorialService {
  final ApiClient _apiClient;
  static const String _keyHistorial = 'historial_compras_exitosas';

  HistorialService({ApiClient? apiClient})
      : _apiClient = apiClient ?? ApiClient();

  /// Guarda una compra en el historial local persistente
  /// Solo se deben registrar compras con estado EXITOSA
  Future<void> registrarCompraExitosa(PagoServicioResponse pago) async {
    if (!pago.isExitosa) return;

    final comprasList = await _obtenerComprasExitosasLocales();

    // Evitar duplicados por idempotencyKey o idTransaccion
    final yaExiste = comprasList.any((c) =>
        (c.idempotencyKey != null && c.idempotencyKey == pago.idempotencyKey) ||
        (c.idTransaccion != null && c.idTransaccion == pago.idTransaccion));

    if (!yaExiste) {
      comprasList.insert(0, pago);
      final prefs = await SharedPreferences.getInstance();
      final rawList = comprasList.map((c) => c.toJson()).toList();
      await prefs.setString(_keyHistorial, jsonEncode(rawList));
    }
  }

  /// Consulta y devuelve **únicamente las compras exitosas** consultando al backend
  /// y haciendo fallback a caché local si ocurre un error de conexión
  Future<List<PagoServicioResponse>> obtenerComprasExitosas({
    int? clienteId,
    String? numeroCuenta,
  }) async {
    try {
      final body = <String, dynamic>{
        'soloExitosas': true,
        if (clienteId != null) 'clienteId': clienteId,
        if (numeroCuenta != null && numeroCuenta.isNotEmpty) 'numeroCuenta': numeroCuenta,
      };

      final response = await _apiClient.post(
        AppConfig.historialPagosEndpoint,
        body: body,
        requiresAuth: true,
      );

      if (response is List) {
        final list = response
            .map((item) => PagoServicioResponse.fromJson(item as Map<String, dynamic>))
            .where((pago) => pago.isExitosa)
            .toList();

        // Actualizar caché local
        final prefs = await SharedPreferences.getInstance();
        final rawList = list.map((c) => c.toJson()).toList();
        await prefs.setString(_keyHistorial, jsonEncode(rawList));

        return list;
      }
    } catch (_) {
      // Fallback a caché local persistente ante falta de red o error temporal
    }

    return _obtenerComprasExitosasLocales();
  }

  /// Lee el historial almacenado localmente en SharedPreferences
  Future<List<PagoServicioResponse>> _obtenerComprasExitosasLocales() async {
    final prefs = await SharedPreferences.getInstance();
    final rawString = prefs.getString(_keyHistorial);

    if (rawString == null || rawString.trim().isEmpty) {
      return [];
    }

    try {
      final decoded = jsonDecode(rawString);
      if (decoded is List) {
        return decoded
            .map((item) => PagoServicioResponse.fromJson(item as Map<String, dynamic>))
            .where((pago) => pago.isExitosa) // Filtro estricto: solo EXITOSAS
            .toList();
      }
    } catch (_) {
      return [];
    }
    return [];
  }

  /// Limpia el historial (al cerrar sesión o por mantenimiento)
  Future<void> limpiarHistorial() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyHistorial);
  }
}
