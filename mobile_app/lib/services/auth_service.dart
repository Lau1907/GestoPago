import '../../config/app_config.dart';
import '../../core/api/api_client.dart';
import '../../core/storage/secure_storage_service.dart';
import '../../models/auth/login_request.dart';
import '../../models/auth/login_response.dart';
import '../../models/auth/verificacion_2fa_request.dart';

class AuthService {
  final ApiClient _apiClient;
  final SecureStorageService _storage;

  AuthService({ApiClient? apiClient, SecureStorageService? storage})
      : _apiClient = apiClient ?? ApiClient(),
        _storage = storage ?? SecureStorageService();

  /// Inicio de sesión enviando correo, password y "dispositivo": "MOBILE"
  Future<LoginResponse> login(String correo, String password) async {
    final request = LoginRequest(
      correo: correo,
      password: password,
      dispositivo: AppConfig.deviceChannel, // "MOBILE"
    );

    final responseJson = await _apiClient.post(
      AppConfig.loginEndpoint,
      body: request.toJson(),
      requiresAuth: false,
    );

    final loginResponse = LoginResponse.fromJson(responseJson);

    // Si requiere 2FA, guardamos temporalmente el token temporal y el correo
    if (loginResponse.requiere2FA) {
      if (loginResponse.tokenTemporal != null) {
        await _storage.savePreAuthToken(loginResponse.tokenTemporal!);
      }
      await _storage.saveCorreo(correo);
    } else if (loginResponse.token != null) {
      // Si no requiere 2FA, guardamos directamente el JWT
      await _storage.saveToken(loginResponse.token!);
      await _storage.saveCorreo(correo);
    }

    return loginResponse;
  }

  /// Validación de OTP 6 dígitos enviando correo, código y "dispositivo": "MOBILE"
  Future<LoginResponse> verificar2fa(String correo, String codigoOtp) async {
    final preAuthToken = await _storage.getPreAuthToken();

    final request = Verificacion2faRequest(
      correo: correo,
      codigo: codigoOtp,
      dispositivo: AppConfig.deviceChannel, // "MOBILE"
      tokenTemporal: preAuthToken,
    );

    final responseJson = await _apiClient.post(
      AppConfig.verify2faEndpoint,
      body: request.toJson(),
      requiresAuth: false,
    );

    final loginResponse = LoginResponse.fromJson(responseJson);

    if (loginResponse.token != null) {
      // Guardar el JWT final en almacenamiento seguro cifrado
      await _storage.saveToken(loginResponse.token!);
      await _storage.saveCorreo(correo);
    }

    return loginResponse;
  }

  /// Cierre de sesión seguro y limpieza de credenciales locales
  Future<void> logout() async {
    await _storage.clearAll();
  }

  /// Comprueba si existe un token guardado previamente
  Future<bool> isAuthenticated() async {
    final token = await _storage.getToken();
    return token != null && token.isNotEmpty;
  }
}
