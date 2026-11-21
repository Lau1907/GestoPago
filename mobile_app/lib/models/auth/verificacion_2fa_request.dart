class Verificacion2faRequest {
  final String correo;
  final String codigo;
  final String dispositivo;
  final String? tokenTemporal;

  Verificacion2faRequest({
    required this.correo,
    required this.codigo,
    this.dispositivo = 'MOBILE',
    this.tokenTemporal,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{
      'correo': correo.trim(),
      'codigo': codigo.trim(),
      'dispositivo': dispositivo,
    };
    if (tokenTemporal != null && tokenTemporal!.isNotEmpty) {
      map['tokenTemporal'] = tokenTemporal;
    }
    return map;
  }
}
