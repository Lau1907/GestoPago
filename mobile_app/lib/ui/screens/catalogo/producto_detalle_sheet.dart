import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../config/app_theme.dart';
import '../../../core/utils/formatters.dart';
import '../../../models/gestopago/producto_item.dart';
import '../../../providers/auth_provider.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/custom_text_field.dart';
import '../pago/confirmacion_pago_dialog.dart';

class ProductoDetalleSheet extends StatefulWidget {
  final ProductoItem producto;

  const ProductoDetalleSheet({super.key, required this.producto});

  @override
  State<ProductoDetalleSheet> createState() => _ProductoDetalleSheetState();
}

class _ProductoDetalleSheetState extends State<ProductoDetalleSheet> {
  final _formKey = GlobalKey<FormState>();
  final _referenciaController = TextEditingController();
  final _montoController = TextEditingController();
  final _digitoVerificadorController = TextEditingController();

  @override
  void initState() {
    super.initState();
    if (widget.producto.tienePrecioFijo) {
      _montoController.text = widget.producto.precioFijoValor!.toStringAsFixed(2);
    }
  }

  void _onContinuar() {
    if (!_formKey.currentState!.validate()) return;

    final authProvider = context.read<AuthProvider>();
    final cuenta = authProvider.cuentaSeleccionada;

    if (cuenta == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('No hay una cuenta bancaria activa seleccionada.'),
          backgroundColor: AppTheme.danger,
        ),
      );
      return;
    }

    final double monto = double.parse(_montoController.text.trim());

    if (cuenta.saldo < monto) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Saldo insuficiente (${Formatters.formatCurrency(cuenta.saldo)} disponible para pagar ${Formatters.formatCurrency(monto)}).',
          ),
          backgroundColor: AppTheme.danger,
        ),
      );
      return;
    }

    // Cerrar bottom sheet e invocar modal de confirmación en rootNavigator
    Navigator.pop(context);

    showDialog(
      context: context,
      useRootNavigator: true,
      barrierDismissible: false,
      builder: (_) => ConfirmacionPagoDialog(
        producto: widget.producto,
        cuenta: cuenta,
        clienteId: authProvider.clienteId ?? 1,
        monto: monto,
        referencia: _referenciaController.text.trim(),
        digitoVerificador: _digitoVerificadorController.text.trim().isNotEmpty
            ? _digitoVerificadorController.text.trim()
            : null,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cuenta = context.watch<AuthProvider>().cuentaSeleccionada;

    return Container(
      padding: EdgeInsets.only(
        top: 20,
        left: 20,
        right: 20,
        bottom: MediaQuery.of(context).viewInsets.bottom + 24,
      ),
      decoration: const BoxDecoration(
        color: AppTheme.cardBg,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        border: Border(top: BorderSide(color: AppTheme.borderColor)),
      ),
      child: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Barra indicadora superior
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppTheme.borderSubtle,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // Título del servicio adaptable
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Logo del servicio (imagen de red o ícono fallback)
                  if (widget.producto.urlLogo != null) ...[
                    ClipRRect(
                      borderRadius: BorderRadius.circular(10),
                      child: Image.network(
                        widget.producto.urlLogo!,
                        width: 44,
                        height: 44,
                        fit: BoxFit.contain,
                        errorBuilder: (_, __, ___) => const Icon(
                          Icons.payment,
                          size: 36,
                          color: AppTheme.primaryLight,
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                  ],
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          widget.producto.producto,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.textPrimary,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          widget.producto.servicio,
                          style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: AppTheme.textSecondary),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),
              const Divider(height: 24, color: AppTheme.borderColor),

              // Información de débito bancario adaptable
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.surface,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.borderColor),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.account_balance_wallet_outlined, color: AppTheme.primaryLight),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Cuenta a debitar:',
                            style: TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                          ),
                          Text(
                            cuenta != null ? Formatters.maskAccount(cuenta.numeroCuenta) : 'Sin cuenta',
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                              color: AppTheme.textPrimary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      cuenta != null ? Formatters.formatCurrency(cuenta.saldo) : '\$0.00',
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                        color: AppTheme.accent,
                      ),
                      maxLines: 1,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Campo Referencia (Teléfono, Contrato, Código de barras)
              CustomTextField(
                controller: _referenciaController,
                label: widget.producto.tipoReferencia != null && widget.producto.tipoReferencia!.isNotEmpty
                    ? 'Referencia (${widget.producto.tipoReferencia})'
                    : 'Referencia / Teléfono / Contrato',
                hint: 'Ej. 9981234567',
                prefixIcon: Icons.tag,
                keyboardType: TextInputType.text,
                validator: (val) {
                  if (val == null || val.trim().length < 3) {
                    return 'Ingrese una referencia válida (mínimo 3 caracteres)';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 14),

              // Dígito Verificador si aplica
              if (widget.producto.hasDigitoVerificador) ...[
                CustomTextField(
                  controller: _digitoVerificadorController,
                  label: 'Dígito Verificador (Obligatorio)',
                  hint: 'Ej. 0 o 1',
                  prefixIcon: Icons.pin,
                  keyboardType: TextInputType.text,
                  validator: (val) {
                    if (widget.producto.hasDigitoVerificador &&
                        (val == null || val.trim().isEmpty)) {
                      return 'Este producto requiere dígito verificador';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 14),
              ],

              // Campo Monto
              CustomTextField(
                controller: _montoController,
                label: 'Monto a Pagar (MXN)',
                hint: '0.00',
                prefixIcon: Icons.attach_money,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                enabled: !widget.producto.tienePrecioFijo,
                validator: (val) {
                  if (val == null || val.trim().isEmpty) {
                    return 'El monto es obligatorio';
                  }
                  final parsed = double.tryParse(val.trim());
                  if (parsed == null || parsed <= 0) {
                    return 'Ingrese un monto mayor a cero';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 24),

              CustomButton(
                text: 'Continuar a Confirmación',
                icon: Icons.arrow_forward,
                onPressed: _onContinuar,
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _referenciaController.dispose();
    _montoController.dispose();
    _digitoVerificadorController.dispose();
    super.dispose();
  }
}
