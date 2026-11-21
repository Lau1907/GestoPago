import 'package:shared_preferences/shared_preferences.dart';

class AppConfig {
  static const String _keyBaseUrl = 'backend_base_url';
  
  // URLs por defecto recomendadas
  static const String defaultAndroidEmulatorUrl = 'http://10.0.2.2:8080';
  static const String defaultLoopbackUrl = 'http://127.0.0.1:8080';
  static const String defaultLocalhostUrl = 'http://localhost:8080';
  
  // URL activa en memoria (por defecto 127.0.0.1 o 10.0.2.2)
  static String baseUrl = defaultAndroidEmulatorUrl;
  
  // Timeout de red
  static const Duration requestTimeout = Duration(seconds: 15);
  
  // Identificador de canal para requerimiento de Sesión Única ("MOBILE")
  static const String deviceChannel = 'MOBILE';

  // Endpoints del backend Spring Boot
  static String get loginEndpoint => '$baseUrl/auth/login';
  static String get verify2faEndpoint => '$baseUrl/auth/2fa/verificar';
  static String get verify2faAltEndpoint => '$baseUrl/auth/verificar-2fa';
  
  static String get catalogoProductosEndpoint => '$baseUrl/gestopago/productos';
  static String get pagarServicioEndpoint => '$baseUrl/pagos/servicio';
  static String get pagarServicioAltEndpoint => '$baseUrl/gestopago/pagos/servicio';
  static String get consultarPagoEndpoint => '$baseUrl/pagos/consultar';
  static String get historialPagosEndpoint => '$baseUrl/pagos/historial';
  
  static String get buscarClienteEndpoint => '$baseUrl/clientes/consultar';
  static String get listarCuentasEndpoint => '$baseUrl/cuentas/buscar';

  /// Inicializa la configuración cargando la URL guardada previamente
  static Future<void> initialize() async {
    final prefs = await SharedPreferences.getInstance();
    final savedUrl = prefs.getString(_keyBaseUrl);
    if (savedUrl != null && savedUrl.trim().isNotEmpty) {
      baseUrl = savedUrl.trim();
    }
  }

  /// Guarda y actualiza la URL base
  static Future<void> setBaseUrl(String newUrl) async {
    final normalized = newUrl.trim().replaceAll(RegExp(r'/+$'), '');
    baseUrl = normalized;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyBaseUrl, normalized);
  }
}
