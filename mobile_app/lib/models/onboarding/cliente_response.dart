import 'cuenta_response.dart';

class ClienteResponse {
  final int id;
  final String primerNombre;
  final String? segundoNombre;
  final String apellidoPaterno;
  final String? apellidoMaterno;
  final String? correo;
  final String? curp;
  final String? rfc;
  final bool activo;
  final List<CuentaResponse> cuentas;

  ClienteResponse({
    required this.id,
    required this.primerNombre,
    this.segundoNombre,
    required this.apellidoPaterno,
    this.apellidoMaterno,
    this.correo,
    this.curp,
    this.rfc,
    this.activo = true,
    this.cuentas = const [],
  });

  String get nombreCompleto {
    final parts = [primerNombre, segundoNombre, apellidoPaterno, apellidoMaterno]
        .where((p) => p != null && p.trim().isNotEmpty)
        .join(' ');
    return parts.isEmpty ? 'Cliente Titular' : parts;
  }

  factory ClienteResponse.fromJson(Map<String, dynamic> json) {
    var rawCuentas = json['cuentas'];
    List<CuentaResponse> listaCuentas = [];
    if (rawCuentas is List) {
      listaCuentas = rawCuentas
          .map((c) => CuentaResponse.fromJson(c as Map<String, dynamic>))
          .toList();
    }

    return ClienteResponse(
      id: json['id'] as int? ?? 1,
      primerNombre: json['primerNombre'] as String? ?? '',
      segundoNombre: json['segundoNombre'] as String?,
      apellidoPaterno: json['apellidoPaterno'] as String? ?? '',
      apellidoMaterno: json['apellidoMaterno'] as String?,
      correo: json['correo'] as String?,
      curp: json['curp'] as String?,
      rfc: json['rfc'] as String?,
      activo: json['activo'] == true,
      cuentas: listaCuentas,
    );
  }
}
