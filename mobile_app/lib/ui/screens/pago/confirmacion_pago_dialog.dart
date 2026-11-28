import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../config/app_theme.dart';
import '../../../core/utils/formatters.dart';
import '../../../models/gestopago/producto_item.dart';
import '../../../models/onboarding/cuenta_response.dart';
import '../../../providers/auth_provider.dart';
import '../../../providers/historial_provider.dart';
import '../../../providers/pago_provider.dart';
import '../../widgets/custom_button.dart';
import 'comprobante_pago_screen.dart';

class ConfirmacionPagoDialog extends StatefulWidget {
  final ProductoItem producto;
  final CuentaResponse cuenta;
  final int clienteId;
  final double monto;
  final String referencia;
  final String? digitoVerificador;

  const ConfirmacionPagoDialog({
    super.key,
    required this.producto,
    required this.cuenta,
    required this.clienteId,
    required this.monto,
    required this.referencia,
    this.digitoVerificador,
  });

  @override
  State<ConfirmacionPagoDialog> createState() => _ConfirmacionPagoDialogState();
}

class _ConfirmacionPagoDialogState extends State<ConfirmacionPagoDialog> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<PagoProvider>().iniciarNuevaTransaccion();
    });
  }

  void _onConfirmarPago() async {
    final authProvider = context.read<AuthProvider>();
    if (!authProvider.isAuthenticated) {
      Navigator.pop(context);
      return;
    }

    final pagoProvider = context.read<PagoProvider>();
    try {
      final response = await pagoProvider.ejecutarPago(
        clienteId: widget.clienteId,
        numeroCuenta: widget.cuenta.numeroCuenta,
        idProducto: widget.producto.idProducto,
        monto: widget.monto,
        referencia: widget.referencia,
        digitoVerificador: widget.digitoVerificador,
      );

      if (!mounted) return;

      // ESTADO 1 o 3: COMPRA EXITOSA o RECHAZADA POR PROVEEDOR -> Navegar a ticket/comprobante
      if (response != null &&
          (pagoProvider.estado == EstadoPago.exitoso ||
              pagoProvider.estado == EstadoPago.rechazado)) {
        if (pagoProvider.estado == EstadoPago.exitoso) {
          // Actualizar cuentas en AuthProvider (saldo debitado)
          context.read<AuthProvider>().cargarDatosClienteYCuentas();
          // Recargar historial exclusivo de exitosas
          context.read<HistorialProvider>().cargarHistorial();
        }

        Navigator.pop(context); // Cerrar diálogo
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ComprobantePagoScreen(pago: response),
          ),
        );
      }
    } catch (e) {
      if (!mounted) return;
      // Si ocurrió un 401 / UnauthorizedSessionException, cerrar el diálogo de inmediato
      // sin permitir continuar ni mostrar botón de "Reintentar"
      Navigator.pop(context);
    }
  }

  /// Construye cada fila del detalle evitando el desbordamiento horizontal (Right Overflow)
  /// mediante SizedBox acotado para etiqueta y Expanded con wrap controlado para valores largos.
  Widget _buildDetalleFila(String etiqueta, String valor, {bool isHighlight = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              etiqueta,
              style: const TextStyle(
                fontSize: 13,
                color: AppTheme.textSecondary,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              valor,
              textAlign: TextAlign.end,
              overflow: TextOverflow.ellipsis,
              maxLines: 2,
              style: TextStyle(
                fontSize: 13,
                fontWeight: isHighlight ? FontWeight.bold : FontWeight.w600,
                color: isHighlight ? AppTheme.accent : AppTheme.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    // Si la sesión fue invalidada o el usuario no está autenticado, cerrar de inmediato el diálogo
    if (!authProvider.isAuthenticated) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && Navigator.canPop(context)) {
          Navigator.pop(context);
        }
      });
      return const SizedBox.shrink();
    }

    final pagoProvider = context.watch<PagoProvider>();

    return AlertDialog(
      backgroundColor: AppTheme.cardBg,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: const BorderSide(color: AppTheme.borderColor),
      ),
      insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      titlePadding: const EdgeInsets.fromLTRB(20, 20, 20, 10),
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
      actionsPadding: const EdgeInsets.fromLTRB(20, 10, 20, 16),
      actionsOverflowButtonSpacing: 8,
      title: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: AppTheme.primary.withValues(alpha: 0.16),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppTheme.primary.withValues(alpha: 0.3)),
            ),
            child: const Icon(Icons.shopping_bag_outlined, color: AppTheme.primaryLight, size: 22),
          ),
          const SizedBox(width: 10),
          const Expanded(
            child: Text(
              'Confirmar Compra',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppTheme.textPrimary),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Revise cuidadosamente los detalles de la transacción antes de autorizar el débito:',
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 12),

            // Tarjeta de detalles de la transacción
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.borderColor),
              ),
              child: Column(
                children: [
                  _buildDetalleFila('Servicio', widget.producto.servicio),
                  _buildDetalleFila('Producto', widget.producto.producto),
                  _buildDetalleFila('Referencia', widget.referencia),
                  if (widget.digitoVerificador != null && widget.digitoVerificador!.isNotEmpty)
                    _buildDetalleFila('Dígito Verif.', widget.digitoVerificador!),
                  _buildDetalleFila('Cuenta Origen', Formatters.maskAccount(widget.cuenta.numeroCuenta)),
                  const Divider(height: 16, color: AppTheme.borderColor),
                  _buildDetalleFila(
                    'Importe a Debitar',
                    Formatters.formatCurrency(widget.monto),
                    isHighlight: true,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),

            // Clave de Idempotencia generada
            if (pagoProvider.currentIdempotencyKey != null)
              Text(
                'Clave Idempotencia: ${pagoProvider.currentIdempotencyKey}',
                style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary),
                textAlign: TextAlign.center,
                overflow: TextOverflow.ellipsis,
                maxLines: 1,
              ),

            const SizedBox(height: 12),

            // =========================================================================
            // MANEJO EXPLÍCITO DE LOS 4 ESTADOS DE COMPRA
            // =========================================================================

            // ESTADO: PROCESANDO
            if (pagoProvider.estado == EstadoPago.procesando) ...[
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withValues(alpha: 0.14),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppTheme.primary.withValues(alpha: 0.35)),
                ),
                child: const Row(
                  children: [
                    SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2.2, color: AppTheme.primaryLight),
                    ),
                    SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'Conectando con GestoPago y debitando cuenta bancaria...',
                        style: TextStyle(fontSize: 12, color: AppTheme.primaryLight, fontWeight: FontWeight.w500),
                      ),
                    ),
                  ],
                ),
              ),
            ]
            // ESTADO 2: COMPRA RECHAZADA (Saldo insuficiente, validación o rechazo externo)
            else if (pagoProvider.estado == EstadoPago.rechazado) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.danger.withValues(alpha: 0.14),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppTheme.danger.withValues(alpha: 0.35)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.cancel_outlined, color: AppTheme.danger, size: 20),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Compra Rechazada',
                            style: TextStyle(
                              color: AppTheme.danger,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      pagoProvider.errorMessage ??
                          'La transacción no pudo completarse. Verifique fondos en la cuenta o los datos ingresados.',
                      style: const TextStyle(color: Color(0xFFFCA5A5), fontSize: 12, height: 1.3),
                    ),
                  ],
                ),
              ),
            ]
            // ESTADO 3: COMPRA PENDIENTE / TIMEOUT EN REVISIÓN (HTTP 504 o revisión de GestoPago)
            else if (pagoProvider.estado == EstadoPago.pendienteRevision) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.warning.withValues(alpha: 0.14),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppTheme.warning.withValues(alpha: 0.4)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.pending_actions_outlined, color: AppTheme.warning, size: 20),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Compra en Revisión / Pendiente',
                            style: TextStyle(
                              color: AppTheme.warning,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      pagoProvider.errorMessage ??
                          'El proveedor no respondió a tiempo. Para su seguridad no se reintentó el cobro para evitar duplicidad.',
                      style: const TextStyle(fontSize: 12, color: Color(0xFFFCD34D), height: 1.3),
                    ),
                    const SizedBox(height: 10),
                    Center(
                      child: OutlinedButton.icon(
                        onPressed: () => pagoProvider.reconsultarTransaccion(),
                        icon: const Icon(Icons.sync, size: 16),
                        label: const Text('Reconsultar Estado en Servidor', style: TextStyle(fontSize: 12)),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: const Color(0xFFFCD34D),
                          side: BorderSide(color: AppTheme.warning.withValues(alpha: 0.6)),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ]
            // ESTADO 4: ERROR DE COMUNICACIÓN (Sin conexión de red, timeout o backend inalcanzable)
            else if (pagoProvider.estado == EstadoPago.errorConexion) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.inputBg,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppTheme.borderSubtle),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.wifi_off_outlined, color: AppTheme.textSecondary, size: 20),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Error de Comunicación',
                            style: TextStyle(
                              color: AppTheme.textPrimary,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      pagoProvider.errorMessage ??
                          'No se pudo conectar con el backend de Spring Boot. Verifique que el servicio esté encendido y la URL accesible.',
                      style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12, height: 1.3),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
      actions: [
        if (!pagoProvider.isProcesando)
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
        CustomButton(
          text: _obtenerTextoBoton(pagoProvider.estado),
          isLoading: pagoProvider.isProcesando,
          color: pagoProvider.estado == EstadoPago.rechazado ||
                  pagoProvider.estado == EstadoPago.errorConexion
              ? AppTheme.primary
              : AppTheme.accent,
          icon: pagoProvider.estado == EstadoPago.rechazado ||
                  pagoProvider.estado == EstadoPago.errorConexion
              ? Icons.refresh
              : Icons.check,
          isFullWidth: false,
          onPressed: _onConfirmarPago,
        ),
      ],
    );
  }

  String _obtenerTextoBoton(EstadoPago estado) {
    switch (estado) {
      case EstadoPago.rechazado:
        return 'Reintentar Compra';
      case EstadoPago.errorConexion:
        return 'Reintentar Conexión';
      case EstadoPago.pendienteRevision:
        return 'Reenviar Intento';
      default:
        return 'Autorizar Débito';
    }
  }
}
