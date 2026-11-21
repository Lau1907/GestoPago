import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStorageService {
  static final SecureStorageService _instance = SecureStorageService._internal();
  factory SecureStorageService() => _instance;
  SecureStorageService._internal();

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(
      encryptedSharedPreferences: true,
    ),
    iOptions: IOSOptions(
      accessibility: KeychainAccessibility.first_unlock,
    ),
  );

  static const String _keyToken = 'auth_jwt_token';
  static const String _keyPreAuthToken = 'pre_auth_temp_token';
  static const String _keyCorreo = 'auth_user_email';
  static const String _keyClienteId = 'auth_user_cliente_id';
  static const String _keyNumeroCuenta = 'auth_user_numero_cuenta';

  // Guardar token JWT principal
  Future<void> saveToken(String token) async {
    await _storage.write(key: _keyToken, value: token);
  }

  Future<String?> getToken() async {
    return await _storage.read(key: _keyToken);
  }

  // Guardar token temporal para 2FA
  Future<void> savePreAuthToken(String token) async {
    await _storage.write(key: _keyPreAuthToken, value: token);
  }

  Future<String?> getPreAuthToken() async {
    return await _storage.read(key: _keyPreAuthToken);
  }

  // Guardar correo
  Future<void> saveCorreo(String correo) async {
    await _storage.write(key: _keyCorreo, value: correo);
  }

  Future<String?> getCorreo() async {
    return await _storage.read(key: _keyCorreo);
  }

  // Guardar datos de cliente/cuenta asociados a la sesión
  Future<void> saveClienteId(int clienteId) async {
    await _storage.write(key: _keyClienteId, value: clienteId.toString());
  }

  Future<int?> getClienteId() async {
    final val = await _storage.read(key: _keyClienteId);
    return val != null ? int.tryParse(val) : null;
  }

  Future<void> saveNumeroCuenta(String numeroCuenta) async {
    await _storage.write(key: _keyNumeroCuenta, value: numeroCuenta);
  }

  Future<String?> getNumeroCuenta() async {
    return await _storage.read(key: _keyNumeroCuenta);
  }

  // Limpiar credenciales al cerrar sesión o por 401
  Future<void> clearAll() async {
    await _storage.deleteAll();
  }
}
