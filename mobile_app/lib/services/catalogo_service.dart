import '../../config/app_config.dart';
import '../../core/api/api_client.dart';
import '../../models/gestopago/producto_item.dart';

class CatalogoService {
  final ApiClient _apiClient;

  CatalogoService({ApiClient? apiClient}) : _apiClient = apiClient ?? ApiClient();

  /// Obtiene la lista de productos y servicios de GestoPago desde GET /gestopago/productos
  Future<List<ProductoItem>> obtenerProductos() async {
    final responseJson = await _apiClient.get(
      AppConfig.catalogoProductosEndpoint,
      requiresAuth: true,
    );

    if (responseJson is Map<String, dynamic>) {
      final rawProductos = responseJson['productos'];
      if (rawProductos is List) {
        return rawProductos
            .map((item) => ProductoItem.fromJson(item as Map<String, dynamic>))
            .toList();
      }
    }

    return [];
  }
}
