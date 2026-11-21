class PagoServicioRequest {
  final int clienteId;
  final String numeroCuenta;
  final int idProducto;
  final double monto;
  final String referencia;
  final String? digitoVerificador;
  final String idempotencyKey;

  PagoServicioRequest({
    required this.clienteId,
    required this.numeroCuenta,
    required this.idProducto,
    required this.monto,
    required this.referencia,
    this.digitoVerificador,
    required this.idempotencyKey,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{
      'clienteId': clienteId,
      'numeroCuenta': numeroCuenta,
      'idProducto': idProducto,
      'monto': monto,
      'referencia': referencia.trim(),
      'idempotencyKey': idempotencyKey,
    };
    if (digitoVerificador != null && digitoVerificador!.isNotEmpty) {
      map['digitoVerificador'] = digitoVerificador!.trim();
    }
    return map;
  }
}
