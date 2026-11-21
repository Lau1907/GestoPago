class LoginRequest {
  final String correo;
  final String password;
  final String dispositivo;
  final bool? requerir2fa;

  LoginRequest({
    required this.correo,
    required this.password,
    this.dispositivo = 'MOBILE', // Requerimiento específico Unidad II
    this.requerir2fa,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{
      'correo': correo.trim(),
      'password': password,
      'dispositivo': dispositivo,
    };
    if (requerir2fa != null) {
      map['requerir2fa'] = requerir2fa;
    }
    return map;
  }
}
