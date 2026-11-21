import 'package:flutter/foundation.dart';
import '../../models/gestopago/producto_item.dart';
import '../../services/catalogo_service.dart';

class CatalogoProvider extends ChangeNotifier {
  final CatalogoService _catalogoService;

  List<ProductoItem> _todosLosProductos = [];
  List<ProductoItem> _productosFiltrados = [];
  bool _isLoading = false;
  String? _errorMessage;
  String _filtroTexto = '';
  String? _categoriaSeleccionada;

  List<ProductoItem> get productos => _productosFiltrados;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String get filtroTexto => _filtroTexto;
  String? get categoriaSeleccionada => _categoriaSeleccionada;

  CatalogoProvider({CatalogoService? catalogoService})
      : _catalogoService = catalogoService ?? CatalogoService();

  List<String> get categoriasDisponibles {
    final categorias = _todosLosProductos.map((p) => p.servicio).toSet().toList();
    categorias.sort();
    return categorias;
  }

  Future<void> cargarProductos() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final lista = await _catalogoService.obtenerProductos();
      _todosLosProductos = lista;
      _aplicarFiltros();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  void filtrarPorTexto(String query) {
    _filtroTexto = query;
    _aplicarFiltros();
    notifyListeners();
  }

  void filtrarPorCategoria(String? categoria) {
    if (_categoriaSeleccionada == categoria) {
      _categoriaSeleccionada = null; // Deseleccionar
    } else {
      _categoriaSeleccionada = categoria;
    }
    _aplicarFiltros();
    notifyListeners();
  }

  void _aplicarFiltros() {
    _productosFiltrados = _todosLosProductos.where((p) {
      final coincideTexto = _filtroTexto.isEmpty ||
          p.producto.toLowerCase().contains(_filtroTexto.toLowerCase()) ||
          p.servicio.toLowerCase().contains(_filtroTexto.toLowerCase());

      final coincideCategoria = _categoriaSeleccionada == null ||
          p.servicio == _categoriaSeleccionada;

      return coincideTexto && coincideCategoria;
    }).toList();
  }
}
