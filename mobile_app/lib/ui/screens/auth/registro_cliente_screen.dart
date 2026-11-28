import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../models/onboarding/cliente_create_request.dart';
import '../../../services/cliente_service.dart';

class RegistroClienteScreen extends StatefulWidget {
  const RegistroClienteScreen({super.key});

  @override
  State<RegistroClienteScreen> createState() => _RegistroClienteScreenState();
}

class _RegistroClienteScreenState extends State<RegistroClienteScreen> {
  static const Color _bgDark = Color(0xFF0B1120);
  static const Color _cardDark = Color(0xFF111827);
  static const Color _surfaceDark = Color(0xFF0F172A);
  static const Color _inputDark = Color(0xFF1F2937);
  static const Color _borderDark = Color(0xFF374151);
  static const Color _blueAccent = Color(0xFF2563EB);
  static const Color _greenOk = Color(0xFF10B981);

  static const List<String> _estadosMexico = [
    'Aguascalientes',
    'Baja California',
    'Baja California Sur',
    'Campeche',
    'Chiapas',
    'Chihuahua',
    'Ciudad de México',
    'Coahuila',
    'Colima',
    'Durango',
    'Estado de México',
    'Guanajuato',
    'Guerrero',
    'Hidalgo',
    'Jalisco',
    'Michoacán',
    'Morelos',
    'Nayarit',
    'Nuevo León',
    'Oaxaca',
    'Puebla',
    'Querétaro',
    'Quintana Roo',
    'San Luis Potosí',
    'Sinaloa',
    'Sonora',
    'Tabasco',
    'Tamaulipas',
    'Tlaxcala',
    'Veracruz',
    'Yucatán',
    'Zacatecas',
  ];

  static const List<String> _ocupaciones = [
    'Empleado',
    'Independiente / Freelancer',
    'Empresario',
    'Estudiante',
    'Comerciante',
    'Profesionista',
    'Jubilado',
  ];

  static const Map<String, double> _rangosIngreso = {
    'Menos de \$10,000': 8000.00,
    '\$10,000 - \$20,000': 15000.00,
    '\$20,001 - \$40,000': 30000.00,
    'Más de \$40,000': 50000.00,
  };

  final _formKey = GlobalKey<FormState>();
  final _clienteService = ClienteService();

  int _currentStep = 0;
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  String? _stepError;

  // Paso 1
  final _primerNombreController = TextEditingController();
  final _segundoNombreController = TextEditingController();
  final _apellidoPaternoController = TextEditingController();
  final _apellidoMaternoController = TextEditingController();
  final _fechaNacimientoController = TextEditingController();
  String? _sexo;
  String? _estadoCivil;

  // Paso 2
  final _curpController = TextEditingController();
  final _rfcController = TextEditingController();
  final _telefonoMovilController = TextEditingController();
  final _correoController = TextEditingController();
  String? _ocupacion;
  String? _rangoIngresoKey;

  // Paso 3
  final _codigoPostalController = TextEditingController();
  String? _estadoSeleccionado;
  final _municipioController = TextEditingController();
  final _coloniaController = TextEditingController();
  final _calleController = TextEditingController();
  final _numeroExteriorController = TextEditingController();
  final _numeroInteriorController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  @override
  void dispose() {
    _primerNombreController.dispose();
    _segundoNombreController.dispose();
    _apellidoPaternoController.dispose();
    _apellidoMaternoController.dispose();
    _fechaNacimientoController.dispose();
    _curpController.dispose();
    _rfcController.dispose();
    _telefonoMovilController.dispose();
    _correoController.dispose();
    _codigoPostalController.dispose();
    _municipioController.dispose();
    _coloniaController.dispose();
    _calleController.dispose();
    _numeroExteriorController.dispose();
    _numeroInteriorController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _seleccionarFecha() async {
    final now = DateTime.now();
    final initial = DateTime(now.year - 20, now.month, now.day);
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1920),
      lastDate: now,
    );
    if (picked != null) {
      final y = picked.year.toString().padLeft(4, '0');
      final m = picked.month.toString().padLeft(2, '0');
      final d = picked.day.toString().padLeft(2, '0');
      setState(() {
        _fechaNacimientoController.text = '$y-$m-$d';
      });
    }
  }

  bool _esMayorDeEdad(String fecha) {
    final parsed = DateTime.tryParse(fecha);
    if (parsed == null) return false;
    final hoy = DateTime.now();
    int edad = hoy.year - parsed.year;
    if (hoy.month < parsed.month ||
        (hoy.month == parsed.month && hoy.day < parsed.day)) {
      edad--;
    }
    return edad >= 18;
  }

  String? _validarPaso(int step) {
    final nombreRegex = RegExp(r'^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$');

    if (step == 0) {
      if (!nombreRegex.hasMatch(_primerNombreController.text.trim())) {
        return 'El primer nombre debe tener entre 2 y 50 letras.';
      }
      final seg = _segundoNombreController.text.trim();
      if (seg.isNotEmpty && !nombreRegex.hasMatch(seg)) {
        return 'El segundo nombre debe tener entre 2 y 50 letras.';
      }
      if (!nombreRegex.hasMatch(_apellidoPaternoController.text.trim())) {
        return 'El apellido paterno debe tener entre 2 y 50 letras.';
      }
      if (!nombreRegex.hasMatch(_apellidoMaternoController.text.trim())) {
        return 'El apellido materno debe tener entre 2 y 50 letras.';
      }
      final fecha = _fechaNacimientoController.text.trim();
      if (fecha.isEmpty || !_esMayorDeEdad(fecha)) {
        return 'Debes ser mayor de 18 años para solicitar la apertura.';
      }
      if (_sexo == null) return 'Selecciona tu sexo.';
      if (_estadoCivil == null) return 'Selecciona tu estado civil.';
      return null;
    }

    if (step == 1) {
      final curp = _curpController.text.trim().toUpperCase();
      final rfc = _rfcController.text.trim().toUpperCase();
      final tel = _telefonoMovilController.text.trim();
      final correo = _correoController.text.trim();

      final curpRegex = RegExp(
        r'^[A-Z][AEIOUX][A-Z]{2}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9]\d$',
      );
      final rfcRegex = RegExp(r'^[A-ZÑ&]{3,4}\d{6}[A-Z0-9]{3}$');
      final emailRegex = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$');

      if (!curpRegex.hasMatch(curp)) {
        return 'La CURP no cumple con el formato oficial de 18 caracteres.';
      }
      if (!rfcRegex.hasMatch(rfc)) {
        return 'El RFC debe tener 12 o 13 caracteres válidos.';
      }
      if (!RegExp(r'^\d{10}$').hasMatch(tel)) {
        return 'El teléfono móvil debe tener exactamente 10 dígitos.';
      }
      if (!emailRegex.hasMatch(correo)) {
        return 'Ingresa un correo electrónico válido.';
      }
      if (_ocupacion == null) return 'Selecciona tu ocupación.';
      if (_rangoIngresoKey == null) return 'Selecciona tu rango de ingreso.';
      return null;
    }

    if (step == 2) {
      if (!RegExp(r'^\d{5}$').hasMatch(_codigoPostalController.text.trim())) {
        return 'El código postal debe tener exactamente 5 dígitos.';
      }
      if (_estadoSeleccionado == null) return 'Selecciona tu estado.';
      if (_municipioController.text.trim().isEmpty) {
        return 'Ingresa tu alcaldía o municipio.';
      }
      if (_coloniaController.text.trim().isEmpty) {
        return 'Ingresa tu colonia.';
      }
      if (_calleController.text.trim().isEmpty) {
        return 'Ingresa tu calle.';
      }
      if (_numeroExteriorController.text.trim().isEmpty) {
        return 'Ingresa el número exterior.';
      }

      final pwd = _passwordController.text;
      if (!_pwdHasLen(pwd) ||
          !_pwdHasUpper(pwd) ||
          !_pwdHasLower(pwd) ||
          !_pwdHasDigit(pwd) ||
          !_pwdHasSymbol(pwd)) {
        return 'La contraseña no cumple con todos los requisitos de seguridad.';
      }
      if (pwd != _confirmPasswordController.text) {
        return 'La confirmación de contraseña no coincide.';
      }
      return null;
    }

    return null;
  }

  bool _pwdHasLen(String p) => p.length >= 8 && p.length <= 12;
  bool _pwdHasUpper(String p) => RegExp(r'[A-Z]').hasMatch(p);
  bool _pwdHasLower(String p) => RegExp(r'[a-z]').hasMatch(p);
  bool _pwdHasDigit(String p) => RegExp(r'[0-9]').hasMatch(p);
  bool _pwdHasSymbol(String p) => RegExp(r'[^A-Za-z0-9]').hasMatch(p);

  void _siguientePaso() {
    final err = _validarPaso(_currentStep);
    if (err != null) {
      setState(() => _stepError = err);
      return;
    }
    setState(() {
      _stepError = null;
      _currentStep++;
    });
  }

  void _pasoAnterior() {
    if (_currentStep > 0) {
      setState(() {
        _stepError = null;
        _currentStep--;
      });
    }
  }

  Future<void> _onSolicitarApertura() async {
    final err = _validarPaso(2);
    if (err != null) {
      setState(() => _stepError = err);
      return;
    }

    setState(() {
      _stepError = null;
      _isLoading = true;
    });

    final request = ClienteCreateRequest(
      primerNombre: _primerNombreController.text.trim(),
      segundoNombre: _segundoNombreController.text.trim().isEmpty
          ? null
          : _segundoNombreController.text.trim(),
      apellidoPaterno: _apellidoPaternoController.text.trim(),
      apellidoMaterno: _apellidoMaternoController.text.trim(),
      curp: _curpController.text.trim().toUpperCase(),
      rfc: _rfcController.text.trim().toUpperCase(),
      correo: _correoController.text.trim(),
      fechaNacimiento: _fechaNacimientoController.text.trim(),
      telefonoMovil: _telefonoMovilController.text.trim(),
      telefonoAlternativo: null,
      sexo: _sexo!,
      estadoCivil: _estadoCivil!,
      nacionalidad: 'Mexicana',
      ocupacion: _ocupacion!,
      empresa: 'No especificada',
      ingresoMensual: _rangosIngreso[_rangoIngresoKey!]!,
      domicilio: DomicilioDto(
        calle: _calleController.text.trim(),
        numeroExterior: _numeroExteriorController.text.trim(),
        numeroInterior: _numeroInteriorController.text.trim().isEmpty
            ? null
            : _numeroInteriorController.text.trim(),
        colonia: _coloniaController.text.trim(),
        municipio: _municipioController.text.trim(),
        estado: _estadoSeleccionado!,
        pais: 'México',
        codigoPostal: _codigoPostalController.text.trim(),
      ),
      password: _passwordController.text,
    );

    final ok = await _clienteService.registrarCliente(request);

    if (!mounted) return;
    setState(() => _isLoading = false);

    if (ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            '¡Solicitud de apertura bancaria exitosa! Ya puedes iniciar sesión.',
          ),
          backgroundColor: _greenOk,
        ),
      );
      Navigator.pop(context);
    } else {
      setState(() {
        _stepError =
            'No se pudo completar la solicitud. Verifica tus datos o intenta más tarde.';
      });
    }
  }

  InputDecoration _darkInputDecoration(String label, {String? hint, Widget? suffixIcon}) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      suffixIcon: suffixIcon,
      filled: true,
      fillColor: _inputDark,
      labelStyle: const TextStyle(color: Color(0xFFD1D5DB), fontSize: 13),
      hintStyle: const TextStyle(color: Color(0xFF6B7280), fontSize: 13),
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: const BorderSide(color: _borderDark),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: const BorderSide(color: _borderDark),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: const BorderSide(color: _blueAccent, width: 1.8),
      ),
    );
  }

  Widget _buildStepIndicator(int stepIndex, String label) {
    final bool isCompleted = stepIndex < _currentStep;
    final bool isActive = stepIndex == _currentStep;

    Color circleColor = _inputDark;
    Color borderColor = _borderDark;
    Color textColor = const Color(0xFF9CA3AF);

    if (isCompleted) {
      circleColor = _greenOk;
      borderColor = const Color(0xFF34D399);
      textColor = const Color(0xFF34D399);
    } else if (isActive) {
      circleColor = _blueAccent;
      borderColor = const Color(0xFF60A5FA);
      textColor = const Color(0xFF60A5FA);
    }

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 28,
          height: 28,
          decoration: BoxDecoration(
            color: circleColor,
            shape: BoxShape.circle,
            border: Border.all(color: borderColor, width: 1.5),
          ),
          alignment: Alignment.center,
          child: isCompleted
              ? const Icon(Icons.check, size: 16, color: Colors.white)
              : Text(
                  '${stepIndex + 1}',
                  style: TextStyle(
                    color: isActive ? Colors.white : const Color(0xFF9CA3AF),
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: TextStyle(
            color: textColor,
            fontSize: 12,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }

  Widget _buildStepper() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: const BoxDecoration(
        color: _surfaceDark,
        border: Border(bottom: BorderSide(color: Color(0xFF1F2937))),
      ),
      child: Row(
        children: [
          _buildStepIndicator(0, '1 Personal'),
          Expanded(
            child: Container(
              height: 2,
              margin: const EdgeInsets.symmetric(horizontal: 6),
              color: _currentStep > 0 ? _greenOk : const Color(0xFF1F2937),
            ),
          ),
          _buildStepIndicator(1, '2 Identidad'),
          Expanded(
            child: Container(
              height: 2,
              margin: const EdgeInsets.symmetric(horizontal: 6),
              color: _currentStep > 1 ? _greenOk : const Color(0xFF1F2937),
            ),
          ),
          _buildStepIndicator(2, '3 Domicilio'),
        ],
      ),
    );
  }

  Widget _buildStep1() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Paso 1 de 3: Datos Personales',
          style: TextStyle(
            color: Colors.white,
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: TextFormField(
                controller: _primerNombreController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Primer Nombre *', hint: 'Juan'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: TextFormField(
                controller: _segundoNombreController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Segundo Nombre', hint: 'Opcional'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: TextFormField(
                controller: _apellidoPaternoController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Apellido Paterno *', hint: 'Gómez'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: TextFormField(
                controller: _apellidoMaternoController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Apellido Materno *', hint: 'López'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        TextFormField(
          controller: _fechaNacimientoController,
          readOnly: true,
          onTap: _seleccionarFecha,
          style: const TextStyle(color: Colors.white),
          decoration: _darkInputDecoration(
            'Fecha de Nacimiento (Mayor de 18 años) *',
            hint: 'YYYY-MM-DD',
            suffixIcon: IconButton(
              icon: const Icon(Icons.calendar_today, color: Color(0xFF9CA3AF)),
              onPressed: _seleccionarFecha,
            ),
          ),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _sexo,
                dropdownColor: _cardDark,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Sexo *'),
                items: const [
                  DropdownMenuItem(value: 'H', child: Text('Hombre')),
                  DropdownMenuItem(value: 'M', child: Text('Mujer')),
                ],
                onChanged: (v) => setState(() => _sexo = v),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _estadoCivil,
                dropdownColor: _cardDark,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Estado Civil *'),
                items: const [
                  DropdownMenuItem(value: 'S', child: Text('Soltero(a)')),
                  DropdownMenuItem(value: 'C', child: Text('Casado(a)')),
                  DropdownMenuItem(value: 'D', child: Text('Divorciado(a)')),
                  DropdownMenuItem(value: 'V', child: Text('Viudo(a)')),
                  DropdownMenuItem(value: 'U', child: Text('Unión Libre')),
                ],
                onChanged: (v) => setState(() => _estadoCivil = v),
              ),
            ),
          ],
        ),
        const SizedBox(height: 22),
        ElevatedButton(
          onPressed: _siguientePaso,
          style: ElevatedButton.styleFrom(
            backgroundColor: _blueAccent,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
          child: const Text(
            'Continuar a Identidad Oficial →',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
      ],
    );
  }

  Widget _buildStep2() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Paso 2 de 3: Identidad Fiscal y Contacto',
          style: TextStyle(
            color: Colors.white,
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: TextFormField(
                controller: _curpController,
                maxLength: 18,
                textCapitalization: TextCapitalization.characters,
                style: const TextStyle(color: Colors.white),
                onChanged: (v) {
                  final upper = v.toUpperCase();
                  if (v != upper) {
                    _curpController.value = TextEditingValue(
                      text: upper,
                      selection: TextSelection.collapsed(offset: upper.length),
                    );
                  }
                },
                decoration: _darkInputDecoration('CURP (18 caracteres) *', hint: 'GOGL800101HMNLRR09')
                    .copyWith(counterText: ''),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: TextFormField(
                controller: _rfcController,
                maxLength: 13,
                textCapitalization: TextCapitalization.characters,
                style: const TextStyle(color: Colors.white),
                onChanged: (v) {
                  final upper = v.toUpperCase();
                  if (v != upper) {
                    _rfcController.value = TextEditingValue(
                      text: upper,
                      selection: TextSelection.collapsed(offset: upper.length),
                    );
                  }
                },
                decoration: _darkInputDecoration('RFC (12-13 car.) *', hint: 'GOGJ800101ABC')
                    .copyWith(counterText: ''),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        TextFormField(
          controller: _telefonoMovilController,
          keyboardType: TextInputType.phone,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          maxLength: 10,
          style: const TextStyle(color: Colors.white),
          decoration: _darkInputDecoration('Teléfono Móvil (10 dígitos) *', hint: '5512345678')
              .copyWith(counterText: ''),
        ),
        const SizedBox(height: 14),
        TextFormField(
          controller: _correoController,
          keyboardType: TextInputType.emailAddress,
          style: const TextStyle(color: Colors.white),
          decoration: _darkInputDecoration('Correo Electrónico *', hint: 'usuario@correo.com'),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _ocupacion,
                isExpanded: true,
                dropdownColor: _cardDark,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Ocupación *'),
                items: _ocupaciones
                    .map((o) => DropdownMenuItem(value: o, child: Text(o, overflow: TextOverflow.ellipsis)))
                    .toList(),
                onChanged: (v) => setState(() => _ocupacion = v),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _rangoIngresoKey,
                isExpanded: true,
                dropdownColor: _cardDark,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Rango de Ingreso *'),
                items: _rangosIngreso.keys
                    .map((k) => DropdownMenuItem(value: k, child: Text(k, overflow: TextOverflow.ellipsis)))
                    .toList(),
                onChanged: (v) => setState(() => _rangoIngresoKey = v),
              ),
            ),
          ],
        ),
        const SizedBox(height: 22),
        Row(
          children: [
            OutlinedButton(
              onPressed: _pasoAnterior,
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.white,
                side: const BorderSide(color: _borderDark),
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: const Text('Atrás'),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: ElevatedButton(
                onPressed: _siguientePaso,
                style: ElevatedButton.styleFrom(
                  backgroundColor: _blueAccent,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                ),
                child: const Text(
                  'Continuar a Domicilio →',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildPwdReqItem(String label, bool met) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 16,
          height: 16,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: met ? _greenOk : _inputDark,
            border: Border.all(
              color: met ? const Color(0xFF34D399) : const Color(0xFF4B5563),
            ),
          ),
          child: met
              ? const Icon(Icons.check, size: 11, color: Colors.white)
              : null,
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: met ? const Color(0xFF34D399) : const Color(0xFF9CA3AF),
            fontWeight: met ? FontWeight.w600 : FontWeight.normal,
          ),
        ),
      ],
    );
  }

  Widget _buildStep3() {
    final pwd = _passwordController.text;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Paso 3 de 3: Domicilio y Acceso',
          style: TextStyle(
            color: Colors.white,
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              flex: 2,
              child: TextFormField(
                controller: _codigoPostalController,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                maxLength: 5,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('C.P. (5 dígitos) *', hint: '01234')
                    .copyWith(counterText: ''),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 3,
              child: DropdownButtonFormField<String>(
                initialValue: _estadoSeleccionado,
                isExpanded: true,
                dropdownColor: _cardDark,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Estado *'),
                items: _estadosMexico
                    .map((e) => DropdownMenuItem(value: e, child: Text(e, overflow: TextOverflow.ellipsis)))
                    .toList(),
                onChanged: (v) => setState(() => _estadoSeleccionado = v),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: TextFormField(
                controller: _municipioController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Alcaldía / Municipio *', hint: 'Benito Juárez'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: TextFormField(
                controller: _coloniaController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Colonia *', hint: 'Del Valle'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              flex: 2,
              child: TextFormField(
                controller: _calleController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Calle *', hint: 'Av. Insurgentes'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: TextFormField(
                controller: _numeroExteriorController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Num. Ext. *', hint: '123'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: TextFormField(
                controller: _numeroInteriorController,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('Num. Int.', hint: 'Opc.'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        TextFormField(
          controller: _passwordController,
          obscureText: _obscurePassword,
          maxLength: 12,
          style: const TextStyle(color: Colors.white),
          onChanged: (_) => setState(() {}),
          decoration: _darkInputDecoration(
            'Contraseña de Acceso (8-12 car.) *',
            hint: '••••••••',
            suffixIcon: IconButton(
              icon: Icon(
                _obscurePassword ? Icons.visibility_off : Icons.visibility,
                color: const Color(0xFF9CA3AF),
              ),
              onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
            ),
          ).copyWith(counterText: ''),
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: _surfaceDark,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: const Color(0xFF1F2937)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'REQUISITOS DE SEGURIDAD',
                style: TextStyle(
                  color: Color(0xFF9CA3AF),
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 0.5,
                ),
              ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 16,
                runSpacing: 8,
                children: [
                  _buildPwdReqItem('8-12 caracteres', _pwdHasLen(pwd)),
                  _buildPwdReqItem('Mayúscula (A-Z)', _pwdHasUpper(pwd)),
                  _buildPwdReqItem('Minúscula (a-z)', _pwdHasLower(pwd)),
                  _buildPwdReqItem('Número (0-9)', _pwdHasDigit(pwd)),
                  _buildPwdReqItem('Símbolo (@,#,\$, etc.)', _pwdHasSymbol(pwd)),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        TextFormField(
          controller: _confirmPasswordController,
          obscureText: _obscureConfirmPassword,
          maxLength: 12,
          style: const TextStyle(color: Colors.white),
          decoration: _darkInputDecoration(
            'Confirmar Contraseña *',
            hint: '••••••••',
            suffixIcon: IconButton(
              icon: Icon(
                _obscureConfirmPassword ? Icons.visibility_off : Icons.visibility,
                color: const Color(0xFF9CA3AF),
              ),
              onPressed: () =>
                  setState(() => _obscureConfirmPassword = !_obscureConfirmPassword),
            ),
          ).copyWith(counterText: ''),
        ),
        const SizedBox(height: 22),
        Row(
          children: [
            OutlinedButton(
              onPressed: _isLoading ? null : _pasoAnterior,
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.white,
                side: const BorderSide(color: _borderDark),
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: const Text('Atrás'),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: ElevatedButton(
                onPressed: _isLoading ? null : _onSolicitarApertura,
                style: ElevatedButton.styleFrom(
                  backgroundColor: _blueAccent,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                ),
                child: _isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text(
                        'Solicitar Apertura Bancaria',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgDark,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Container(
            decoration: BoxDecoration(
              color: _cardDark,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: const Color(0xFF1F2937)),
            ),
            clipBehavior: Clip.antiAlias,
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Encabezado
                  Container(
                    padding: const EdgeInsets.all(18),
                    decoration: const BoxDecoration(
                      color: _surfaceDark,
                      border: Border(bottom: BorderSide(color: Color(0xFF1F2937))),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        InkWell(
                          onTap: () => Navigator.pop(context),
                          borderRadius: BorderRadius.circular(8),
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                            decoration: BoxDecoration(
                              color: _inputDark,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(color: _borderDark),
                            ),
                            child: const Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(Icons.arrow_back, size: 15, color: Color(0xFF93C5FD)),
                                SizedBox(width: 6),
                                Text(
                                  'Solicitud de Apertura Bancaria',
                                  style: TextStyle(
                                    color: Color(0xFF93C5FD),
                                    fontSize: 12,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: 14),
                        Row(
                          children: [
                            Container(
                              width: 44,
                              height: 44,
                              decoration: BoxDecoration(
                                color: _blueAccent,
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: const Icon(
                                Icons.account_balance,
                                color: Colors.white,
                                size: 24,
                              ),
                            ),
                            const SizedBox(width: 12),
                            const Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'Apertura de Cuenta Digital',
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontSize: 18,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  SizedBox(height: 2),
                                  Text(
                                    'Proceso de Onboarding Oficial KYC',
                                    style: TextStyle(
                                      color: Color(0xFF9CA3AF),
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),

                  // Stepper
                  _buildStepper(),

                  // Cuerpo del paso actual
                  Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        if (_stepError != null)
                          Container(
                            margin: const EdgeInsets.only(bottom: 16),
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.red.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: Colors.red.shade400.withValues(alpha: 0.5)),
                            ),
                            child: Text(
                              _stepError!,
                              style: TextStyle(color: Colors.red.shade200, fontSize: 13),
                            ),
                          ),
                        if (_currentStep == 0) _buildStep1(),
                        if (_currentStep == 1) _buildStep2(),
                        if (_currentStep == 2) _buildStep3(),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
