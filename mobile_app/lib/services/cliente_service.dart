import '../../config/app_config.dart';
import '../../core/api/api_client.dart';
import '../../models/onboarding/cliente_response.dart';
import '../../models/onboarding/cuenta_response.dart';

class ClienteService {
  final ApiClient _apiClient;

  ClienteService({ApiClient? apiClient}) : _apiClient = apiClient ?? ApiClient();

  /// Consulta los datos del cliente por ID (POST /clientes/consultar)
  Future<ClienteResponse?> obtenerClientePorId(int clienteId) async {
    try {
      final responseJson = await _apiClient.post(
        AppConfig.buscarClienteEndpoint,
        body: {'id': clienteId},
        requiresAuth: true,
      );

      if (responseJson is Map<String, dynamic>) {
        return ClienteResponse.fromJson(responseJson);
      }
    } catch (_) {
      // Si falla la consulta por ID, devolvemos null para fallback
    }
    return null;
  }

  /// Consulta la lista de cuentas bancarias activas (POST /cuentas/buscar)
  Future<List<CuentaResponse>> obtenerCuentas() async {
    try {
      final responseJson = await _apiClient.post(
        AppConfig.listarCuentasEndpoint,
        body: {},
        requiresAuth: true,
      );

      if (responseJson is List) {
        return responseJson
            .map((c) => CuentaResponse.fromJson(c as Map<String, dynamic>))
            .where((c) => c.isActiva)
            .toList();
      }
    } catch (_) {
      // Fallback
    }
    return [];
  }
}
