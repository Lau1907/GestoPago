import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import '../storage/secure_storage_service.dart';
import 'api_exceptions.dart';

typedef SessionInvalidatedCallback = void Function(String message);

class ApiClient {
  final http.Client _client = http.Client();
  final SecureStorageService _storage = SecureStorageService();
  
  // Callback global registrado cuando se detecta un 401 por Sesión Única invalidada
  static SessionInvalidatedCallback? onSessionInvalidated;

  /// Realiza peticiones POST centralizadas con validación de status y captura de 401
  Future<dynamic> post(
    String url, {
    Map<String, dynamic>? body,
    bool requiresAuth = true,
    Map<String, String>? extraHeaders,
  }) async {
    final uri = Uri.parse(url);
    final headers = await _buildHeaders(requiresAuth, extraHeaders);

    try {
      final response = await _client
          .post(
            uri,
            headers: headers,
            body: body != null ? jsonEncode(body) : null,
          )
          .timeout(const Duration(seconds: 15));

      return await _processResponse(response);
    } on SocketException {
      throw NetworkException(
        'No se pudo conectar con el servidor ($url). Verifique su conexión de red y que el backend de Spring Boot esté activo.',
      );
    } on TimeoutException {
      throw NetworkException(
        'Tiempo de espera agotado al conectar con el servidor. Por favor intente nuevamente.',
      );
    } on FormatException {
      throw ServerException('Respuesta del servidor con formato no válido.');
    }
  }

  /// Realiza peticiones GET centralizadas
  Future<dynamic> get(
    String url, {
    bool requiresAuth = true,
    Map<String, String>? extraHeaders,
  }) async {
    final uri = Uri.parse(url);
    final headers = await _buildHeaders(requiresAuth, extraHeaders);

    try {
      final response = await _client
          .get(uri, headers: headers)
          .timeout(const Duration(seconds: 15));

      return await _processResponse(response);
    } on SocketException {
      throw NetworkException(
        'No se pudo conectar con el servidor ($url). Verifique su conexión de red y que el backend esté activo.',
      );
    } on TimeoutException {
      throw NetworkException('Tiempo de espera agotado al consultar el servidor.');
    } on FormatException {
      throw ServerException('Respuesta del servidor con formato no válido.');
    }
  }

  Future<Map<String, String>> _buildHeaders(
    bool requiresAuth,
    Map<String, String>? extraHeaders,
  ) async {
    final headers = <String, String>{
      'Content-Type': 'application/json; charset=UTF-8',
      'Accept': 'application/json',
    };

    if (requiresAuth) {
      final token = await _storage.getToken();
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      } else {
        // SEGURIDAD: Nunca enviar peticiones que exigen autenticación sin token (anónimas)
        onSessionInvalidated?.call('No hay sesión activa. Debe iniciar sesión nuevamente.');
        throw UnauthorizedSessionException(
          'No hay sesión activa o fue invalidada. Inicie sesión nuevamente.',
          sessionClosedByAnotherDevice: true,
        );
      }
    }

    if (extraHeaders != null) {
      headers.addAll(extraHeaders);
    }

    return headers;
  }

  Future<dynamic> _processResponse(http.Response response) async {
    dynamic jsonBody;
    try {
      if (response.body.isNotEmpty) {
        jsonBody = jsonDecode(utf8.decode(response.bodyBytes));
      }
    } catch (_) {
      jsonBody = null;
    }

    final statusCode = response.statusCode;

    if (statusCode >= 200 && statusCode < 300) {
      return jsonBody;
    }

    // Manejo específico del 401: Control de Sesión Única invalidada
    if (statusCode == 401) {
      String mensaje = 'Sesión inválida o expirada.';
      bool closedByOther = false;

      if (jsonBody is Map<String, dynamic>) {
        mensaje = jsonBody['mensaje'] ?? jsonBody['message'] ?? mensaje;
      }

      if (mensaje.contains('otro dispositivo') ||
          mensaje.contains('otra plataforma') ||
          mensaje.contains('cerrada') ||
          mensaje.contains('inválida') ||
          mensaje.contains('invalida')) {
        closedByOther = true;
      }

      // Limpieza inmediata y segura del token en FlutterSecureStorage ante 401
      await _storage.clearAll();

      // Disparar evento para que la UI limpie el estado en memoria y redirija al Login
      onSessionInvalidated?.call(mensaje);

      throw UnauthorizedSessionException(
        mensaje,
        sessionClosedByAnotherDevice: closedByOther,
      );
    }

    // Errores de negocio y validación (HTTP 400, 404, 409, 422) -> COMPRA RECHAZADA
    if (statusCode == 400 || statusCode == 404 || statusCode == 409 || statusCode == 422) {
      String mensaje = 'La solicitud no pudo ser procesada.';
      if (jsonBody is Map<String, dynamic>) {
        mensaje = jsonBody['mensaje'] ?? jsonBody['message'] ?? mensaje;
      }
      throw ValidationException(mensaje, statusCode);
    }

    // Timeout o transacción en revisión (HTTP 504) -> COMPRA PENDIENTE
    if (statusCode == 504) {
      String mensaje = 'Tiempo de espera agotado con el proveedor externo. La transacción está en revisión.';
      if (jsonBody is Map<String, dynamic>) {
        mensaje = jsonBody['mensaje'] ?? jsonBody['message'] ?? mensaje;
      }
      throw ServerException(mensaje, 504);
    }

    // Error interno del servidor u otros 5xx
    String errorMsg = 'Error en el servidor ($statusCode).';
    if (jsonBody is Map<String, dynamic>) {
      errorMsg = jsonBody['mensaje'] ?? jsonBody['message'] ?? errorMsg;
    }
    throw ServerException(errorMsg, statusCode);
  }
}
