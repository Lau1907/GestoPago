class AppException implements Exception {
  final String message;
  final int? statusCode;

  AppException(this.message, [this.statusCode]);

  @override
  String toString() => message;
}

/// Excepción cuando el backend invalida la sesión (HTTP 401)
/// por haber iniciado sesión en otro dispositivo ("WEB" u otro móvil).
class UnauthorizedSessionException extends AppException {
  final bool sessionClosedByAnotherDevice;

  UnauthorizedSessionException(
    String message, {
    this.sessionClosedByAnotherDevice = false,
  }) : super(message, 401);
}

/// Error de comunicación de red (SocketException, timeout, servidor inalcanzable)
class NetworkException extends AppException {
  NetworkException(String message) : super(message, 503);
}

/// Error devuelto por la lógica de negocio o validación (HTTP 400, 409, 422)
/// Representa una COMPRA RECHAZADA por el backend
class ValidationException extends AppException {
  ValidationException(String message, [int? statusCode]) : super(message, statusCode ?? 400);
}

/// Error del servidor (HTTP 500, 502, 504)
/// Cuando el status es 504 o menciona timeout/revisión representa una COMPRA PENDIENTE
class ServerException extends AppException {
  ServerException(String message, [int? statusCode]) : super(message, statusCode ?? 500);

  bool get isPendingReview =>
      statusCode == 504 ||
      message.toLowerCase().contains('revisión') ||
      message.toLowerCase().contains('revision') ||
      message.toLowerCase().contains('timeout');
}
