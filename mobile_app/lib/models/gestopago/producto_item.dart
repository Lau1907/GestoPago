class ProductoItem {
  final String servicio;
  final String producto;
  final int idServicio;
  final int idProducto;
  final int? idCatTipoServicio;
  final int? tipoFront;
  final bool hasDigitoVerificador;
  final String? precio;
  final bool showAyuda;
  final String? tipoReferencia;
  final String? legend;

  ProductoItem({
    required this.servicio,
    required this.producto,
    required this.idServicio,
    required this.idProducto,
    this.idCatTipoServicio,
    this.tipoFront,
    this.hasDigitoVerificador = false,
    this.precio,
    this.showAyuda = false,
    this.tipoReferencia,
    this.legend,
  });

  factory ProductoItem.fromJson(Map<String, dynamic> json) {
    return ProductoItem(
      servicio: json['servicio'] as String? ?? 'Servicio',
      producto: json['producto'] as String? ?? 'Producto',
      idServicio: json['idServicio'] is int
          ? json['idServicio'] as int
          : int.tryParse(json['idServicio']?.toString() ?? '0') ?? 0,
      idProducto: json['idProducto'] is int
          ? json['idProducto'] as int
          : int.tryParse(json['idProducto']?.toString() ?? '0') ?? 0,
      idCatTipoServicio: json['idCatTipoServicio'] is int
          ? json['idCatTipoServicio'] as int
          : int.tryParse(json['idCatTipoServicio']?.toString() ?? ''),
      tipoFront: json['tipoFront'] is int
          ? json['tipoFront'] as int
          : int.tryParse(json['tipoFront']?.toString() ?? ''),
      hasDigitoVerificador: json['hasDigitoVerificador'] == true,
      precio: json['precio'] as String?,
      showAyuda: json['showAyuda'] == true,
      tipoReferencia: json['tipoReferencia'] as String?,
      legend: json['legend'] as String?,
    );
  }

  /// Indica si tiene precio fijo numérico
  bool get tienePrecioFijo {
    if (precio == null || precio!.trim().isEmpty) return false;
    final val = double.tryParse(precio!.trim());
    return val != null && val > 0;
  }

  double? get precioFijoValor {
    if (precio == null) return null;
    return double.tryParse(precio!.trim());
  }
}
