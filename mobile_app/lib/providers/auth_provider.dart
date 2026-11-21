import 'package:flutter/foundation.dart';
import '../../core/storage/secure_storage_service.dart';
import '../../models/auth/login_response.dart';
import '../../models/onboarding/cliente_response.dart';
import '../../models/onboarding/cuenta_response.dart';
import '../../services/auth_service.dart';
import '../../services/cliente_service.dart';

enum AuthStatus { unauthenticated, authenticating, requiring2FA, authenticated }

class AuthProvider extends ChangeNotifier {
  final AuthService _authService;
  final ClienteService _clienteService;
  final SecureStorageService _storage;

  AuthStatus _status = AuthStatus.unauthenticated;
  bool _isLoading = false;
  String? _errorMessage;
  String? _sessionInvalidatedMessage;

  String? _currentEmail;
  String? _temporalOtpForDev; // Código OTP para agilizar pruebas en desarrollo
  int? _clienteId;
  ClienteResponse? _cliente;
  List<CuentaResponse> _cuentas = [];
  CuentaResponse? _cuentaSeleccionada;

  AuthStatus get status => _status;
  bool get isLoading => _isLoading;
  bool get isAuthenticated => _status == AuthStatus.authenticated;
  bool get requires2FA => _status == AuthStatus.requiring2FA;
  String? get errorMessage => _errorMessage;
  String? get sessionInvalidatedMessage => _sessionInvalidatedMessage;
  String? get currentEmail => _currentEmail;
  String? get temporalOtpForDev => _temporalOtpForDev;
  int? get clienteId => _clienteId;
  ClienteResponse? get cliente => _cliente;
  List<CuentaResponse> get cuentas => _cuentas;
  CuentaResponse? get cuentaSeleccionada => _cuentaSeleccionada;

  AuthProvider({
    AuthService? authService,
    ClienteService? clienteService,
    SecureStorageService? storage,
  })  : _authService = authService ?? AuthService(),
        _clienteService = clienteService ?? ClienteService(),
        _storage = storage ?? SecureStorageService();

  /// Verifica si ya había sesión iniciada al arrancar la app
  Future<void> checkAuthStatus() async {
    final hasToken = await _authService.isAuthenticated();
    if (hasToken) {
      _currentEmail = await _storage.getCorreo();
      _clienteId = await _storage.getClienteId();
      _status = AuthStatus.authenticated;
      notifyListeners();
      await cargarDatosClienteYCuentas();
    } else {
      _status = AuthStatus.unauthenticated;
      notifyListeners();
    }
  }

  /// Login con correo, password y dispositivo "MOBILE" (Sesión Única)
  Future<bool> login(String correo, String password) async {
    _isLoading = true;
    _errorMessage = null;
    _sessionInvalidatedMessage = null;
    notifyListeners();

    try {
      final LoginResponse response = await _authService.login(correo, password);
      _currentEmail = correo;
      _temporalOtpForDev = response.codigoOtp;

      if (response.requiere2FA) {
        _status = AuthStatus.requiring2FA;
        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        _status = AuthStatus.authenticated;
        _isLoading = false;
        notifyListeners();
        await cargarDatosClienteYCuentas();
        return true;
      }
    } catch (e) {
      _isLoading = false;
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      notifyListeners();
      return false;
    }
  }

  /// Validación de OTP 6 dígitos
  Future<bool> verificar2fa(String codigoOtp) async {
    if (_currentEmail == null) {
      _errorMessage = 'Correo no especificado para validación 2FA.';
      notifyListeners();
      return false;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final response = await _authService.verificar2fa(_currentEmail!, codigoOtp);
      if (response.token != null) {
        _status = AuthStatus.authenticated;
        _temporalOtpForDev = null;
        _isLoading = false;
        notifyListeners();
        await cargarDatosClienteYCuentas();
        return true;
      } else {
        _errorMessage = response.mensaje ?? 'No se recibió token de acceso.';
        _isLoading = false;
        notifyListeners();
        return false;
      }
    } catch (e) {
      _isLoading = false;
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      notifyListeners();
      return false;
    }
  }

  /// Carga datos del cliente y sus cuentas bancarias activas
  Future<void> cargarDatosClienteYCuentas() async {
    try {
      // 1. Obtener cuentas activas del sistema
      final cuentasObtenidas = await _clienteService.obtenerCuentas();
      if (cuentasObtenidas.isNotEmpty) {
        _cuentas = cuentasObtenidas;
        _cuentaSeleccionada = _cuentas.first;
        await _storage.saveNumeroCuenta(_cuentaSeleccionada!.numeroCuenta);
      }

      // 2. Cargar cliente por ID predeterminado (1) o guardado
      int idToSearch = _clienteId ?? 1;
      final cli = await _clienteService.obtenerClientePorId(idToSearch);
      if (cli != null) {
        _cliente = cli;
        _clienteId = cli.id;
        await _storage.saveClienteId(cli.id);
        if (cli.cuentas.isNotEmpty && _cuentas.isEmpty) {
          _cuentas = cli.cuentas.where((c) => c.isActiva).toList();
          if (_cuentas.isNotEmpty) {
            _cuentaSeleccionada = _cuentas.first;
          }
        }
      }
      notifyListeners();
    } catch (_) {
      // Tolerancia a fallos de red en carga secundaria
    }
  }

  void seleccionarCuenta(CuentaResponse cuenta) {
    _cuentaSeleccionada = cuenta;
    _storage.saveNumeroCuenta(cuenta.numeroCuenta);
    notifyListeners();
  }

  void clearSessionInvalidatedMessage() {
    _sessionInvalidatedMessage = null;
    notifyListeners();
  }

  /// Manejo explícito cuando el backend invalida la sesión (HTTP 401 por sesión abierta en otro dispositivo)
  Future<void> handleSessionInvalidated(String mensaje) async {
    _sessionInvalidatedMessage = mensaje;
    await _limpiarEstadoYSesionSegura();
  }

  /// Cierre de sesión seguro y manual
  Future<void> logout() async {
    await _limpiarEstadoYSesionSegura();
  }

  /// Borra tokens en FlutterSecureStorage y limpia el estado en memoria de forma atómica
  Future<void> _limpiarEstadoYSesionSegura() async {
    await _authService.logout(); // Ejecuta _storage.clearAll()
    _status = AuthStatus.unauthenticated;
    _currentEmail = null;
    _clienteId = null;
    _cliente = null;
    _cuentas = [];
    _cuentaSeleccionada = null;
    _temporalOtpForDev = null;
    _isLoading = false;
    notifyListeners();
  }
}
