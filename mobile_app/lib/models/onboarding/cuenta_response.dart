class CuentaResponse {
  final int? id;
  final String numeroCuenta;
  final double saldo;
  final String estatus;

  CuentaResponse({
    this.id,
    required this.numeroCuenta,
    required this.saldo,
    required this.estatus,
  });

  factory CuentaResponse.fromJson(Map<String, dynamic> json) {
    return CuentaResponse(
      id: json['id'] as int?,
      numeroCuenta: json['numeroCuenta'] as String? ?? '',
      saldo: json['saldo'] is num ? (json['saldo'] as num).toDouble() : 0.0,
      estatus: json['estatus'] as String? ?? 'A',
    );
  }

  bool get isActiva => estatus == 'ACTIVA' || estatus == 'A';
}
