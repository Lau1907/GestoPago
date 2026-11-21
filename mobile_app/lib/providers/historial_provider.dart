import 'package:flutter/foundation.dart';
import '../../models/gestopago/pago_response.dart';
import '../../services/historial_service.dart';

class HistorialProvider extends ChangeNotifier {
  final HistorialService _historialService;

  List<PagoServicioResponse> _comprasExitosas = [];
  bool _isLoading = false;
  String? _errorMessage;
  int? _lastClienteId;
  String? _lastNumeroCuenta;

  List<PagoServicioResponse> get comprasExitosas => _comprasExitosas;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  HistorialProvider({HistorialService? historialService})
      : _historialService = historialService ?? HistorialService();

  /// Consulta y filtra **exclusivamente las compras exitosas**
  Future<void> cargarHistorial({int? clienteId, String? numeroCuenta}) async {
    if (clienteId != null) _lastClienteId = clienteId;
    if (numeroCuenta != null) _lastNumeroCuenta = numeroCuenta;

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final list = await _historialService.obtenerComprasExitosas(
        clienteId: _lastClienteId,
        numeroCuenta: _lastNumeroCuenta,
      );
      // Garantizar que únicamente se muestren compras exitosas
      _comprasExitosas = list.where((c) => c.isExitosa).toList();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  Future<void> limpiar() async {
    await _historialService.limpiarHistorial();
    _comprasExitosas = [];
    _lastClienteId = null;
    _lastNumeroCuenta = null;
    notifyListeners();
  }
}
