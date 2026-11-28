class LoginResponse {
  final String? token;
  final String? tipo;
  final int? expiraEn;
  final bool requiere2FA;
  final String? tokenTemporal;
  final String? dispositivo;
  final String? mensaje;

  LoginResponse({
    this.token,
    this.tipo,
    this.expiraEn,
    this.requiere2FA = false,
    this.tokenTemporal,
    this.dispositivo,
    this.mensaje,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      token: json['token'] as String?,
      tipo: json['tipo'] as String?,
      expiraEn: json['expiraEn'] as int?,
      requiere2FA: json['requiere2FA'] == true,
      tokenTemporal: json['tokenTemporal'] as String?,
      dispositivo: json['dispositivo'] as String?,
      mensaje: json['mensaje'] as String?,
    );
  }
}
