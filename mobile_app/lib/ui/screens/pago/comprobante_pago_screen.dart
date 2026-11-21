import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import '../../../core/utils/formatters.dart';
import '../../../models/gestopago/pago_response.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/status_badge.dart';

class ComprobantePagoScreen extends StatelessWidget {
  final PagoServicioResponse pago;

  const ComprobantePagoScreen({super.key, required this.pago});

  Widget _buildTicketRow(String label, String value, {bool isBold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 125,
            child: Text(
              label,
              style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.end,
              overflow: TextOverflow.ellipsis,
              maxLines: 3,
              style: TextStyle(
                fontSize: 13,
                fontWeight: isBold ? FontWeight.bold : FontWeight.w600,
                color: AppTheme.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Comprobante de Pago'),
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Encabezado de éxito
              Center(
                child: Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: AppTheme.accent.withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.check_circle,
                    size: 46,
                    color: AppTheme.accent,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                '¡Pago Aplicado con Éxito!',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 4),
              Center(
                child: StatusBadge(estado: pago.estado ?? 'EXITOSA'),
              ),
              const SizedBox(height: 16),

              // Monto destacado
              Center(
                child: Text(
                  Formatters.formatCurrency(pago.monto),
                  style: const TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.w900,
                    color: AppTheme.primary,
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // Ticket Bancario Estilizado
              Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.06),
                      blurRadius: 10,
                      offset: const Offset(0, 4),
                    ),
                  ],
                  border: Border.all(color: Colors.grey.shade200),
                ),
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                      'Detalle de la Operación GestoPago',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: AppTheme.primary,
                      ),
                    ),
                    const Divider(height: 20),

                    _buildTicketRow('Servicio', pago.servicio ?? 'GestoPago'),
                    _buildTicketRow('Producto', pago.producto ?? '-'),
                    _buildTicketRow('Referencia', pago.referencia ?? '-'),
                    _buildTicketRow('Folio GestoPago', pago.folioGestoPago ?? 'GP-${pago.idTransaccion ?? '001'}', isBold: true),
                    if (pago.autorizacionGestoPago != null)
                      _buildTicketRow('No. Autorización', pago.autorizacionGestoPago!),
                    _buildTicketRow('Cuenta Debitada', Formatters.maskAccount(pago.numeroCuenta)),
                    if (pago.saldoAnterior != null)
                      _buildTicketRow('Saldo Anterior', Formatters.formatCurrency(pago.saldoAnterior)),
                    if (pago.saldoNuevo != null)
                      _buildTicketRow('Saldo Nuevo', Formatters.formatCurrency(pago.saldoNuevo), isBold: true),
                    _buildTicketRow('Fecha y Hora', Formatters.formatDateTime(pago.fecha ?? DateTime.now().toIso8601String())),
                    _buildTicketRow('Clave Idempotencia', pago.idempotencyKey ?? '-'),

                    if (pago.ticket != null && pago.ticket!.isNotEmpty) ...[
                      const Divider(height: 24),
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppTheme.surface,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          'Ticket / Leyenda:\n${pago.ticket}',
                          style: TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 11,
                            color: Colors.grey.shade700,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    ],
                  ],
                ),
              ),

              const SizedBox(height: 28),

              CustomButton(
                text: 'Volver al Inicio',
                icon: Icons.home_outlined,
                onPressed: () {
                  Navigator.popUntil(context, (route) => route.isFirst);
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
