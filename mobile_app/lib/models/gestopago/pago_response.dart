class PagoServicioResponse {
  final int? idTransaccion;
  final String? idempotencyKey;
  final String? numeroCuenta;
  final int? clienteId;
  final int? idProducto;
  final String? servicio;
  final String? producto;
  final double? monto;
  final double? saldoAnterior;
  final double? saldoNuevo;
  final String? referencia;
  final String? estado; // EXITOSA, FALLIDA, TIMEOUT_REVISION
  final String? codigoGestoPago;
  final String? mensajeGestoPago;
  final String? folioGestoPago;
  final String? idTransaccionProveedor;
  final String? codigoUpc;
  final String? autorizacionGestoPago;
  final String? ticket;
  final String? fecha;
  final String? urlLogo;

  PagoServicioResponse({
    this.idTransaccion,
    this.idempotencyKey,
    this.numeroCuenta,
    this.clienteId,
    this.idProducto,
    this.servicio,
    this.producto,
    this.monto,
    this.saldoAnterior,
    this.saldoNuevo,
    this.referencia,
    this.estado,
    this.codigoGestoPago,
    this.mensajeGestoPago,
    this.folioGestoPago,
    this.idTransaccionProveedor,
    this.codigoUpc,
    this.autorizacionGestoPago,
    this.ticket,
    this.fecha,
    this.urlLogo,
  });

  factory PagoServicioResponse.fromJson(Map<String, dynamic> json) {
    return PagoServicioResponse(
      idTransaccion: json['idTransaccion'] as int?,
      idempotencyKey: json['idempotencyKey'] as String?,
      numeroCuenta: json['numeroCuenta'] as String?,
      clienteId: json['clienteId'] as int?,
      idProducto: json['idProducto'] as int?,
      servicio: json['servicio'] as String?,
      producto: json['producto'] as String?,
      monto: json['monto'] is num ? (json['monto'] as num).toDouble() : null,
      saldoAnterior: json['saldoAnterior'] is num
          ? (json['saldoAnterior'] as num).toDouble()
          : null,
      saldoNuevo: json['saldoNuevo'] is num
          ? (json['saldoNuevo'] as num).toDouble()
          : null,
      referencia: json['referencia'] as String?,
      estado: json['estado'] as String?,
      codigoGestoPago: json['codigoGestoPago'] as String?,
      mensajeGestoPago: json['mensajeGestoPago'] as String?,
      folioGestoPago: json['folioGestoPago'] as String?,
      idTransaccionProveedor: json['idTransaccionProveedor'] as String?,
      codigoUpc: json['codigoUpc'] as String?,
      autorizacionGestoPago: json['autorizacionGestoPago'] as String?,
      ticket: json['ticket'] as String?,
      fecha: json['fecha'] as String?,
      urlLogo: json['urlLogo'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'idTransaccion': idTransaccion,
      'idempotencyKey': idempotencyKey,
      'numeroCuenta': numeroCuenta,
      'clienteId': clienteId,
      'idProducto': idProducto,
      'servicio': servicio,
      'producto': producto,
      'monto': monto,
      'saldoAnterior': saldoAnterior,
      'saldoNuevo': saldoNuevo,
      'referencia': referencia,
      'estado': estado,
      'codigoGestoPago': codigoGestoPago,
      'mensajeGestoPago': mensajeGestoPago,
      'folioGestoPago': folioGestoPago,
      'idTransaccionProveedor': idTransaccionProveedor,
      'codigoUpc': codigoUpc,
      'autorizacionGestoPago': autorizacionGestoPago,
      'ticket': ticket,
      'fecha': fecha,
      'urlLogo': urlLogo,
    };
  }

  bool get isExitosa => estado?.toUpperCase() == 'EXITOSA';
  bool get isPendiente => estado?.toUpperCase() == 'TIMEOUT_REVISION' || estado?.toUpperCase() == 'INICIADA';
  bool get isRechazada => estado?.toUpperCase() == 'FALLIDA';
}
