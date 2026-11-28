import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import '../../../core/utils/formatters.dart';
import '../../../models/gestopago/pago_response.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/status_badge.dart';

class ComprobantePagoScreen extends StatelessWidget {
  final PagoServicioResponse pago;

  const ComprobantePagoScreen({super.key, required this.pago});

  IconData _iconoServicio(String? servicio) {
    final s = (servicio ?? '').toLowerCase();
    if (s.contains('agua')) return Icons.water_drop_outlined;
    if (s.contains('luz') || s.contains('cfe') || s.contains('energia')) {
      return Icons.bolt_outlined;
    }
    if (s.contains('gas')) return Icons.local_fire_department_outlined;
    if (s.contains('tel') || s.contains('movil') || s.contains('recarga')) {
      return Icons.smartphone_outlined;
    }
    if (s.contains('tv') || s.contains('cable') || s.contains('sky')) {
      return Icons.tv_outlined;
    }
    if (s.contains('internet')) return Icons.wifi_outlined;
    return Icons.receipt_long_outlined;
  }

  Widget _buildTicketRow(String label, String value, {bool isBold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 135,
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
    final bool esExitosa = pago.isExitosa;
    final Color statusColor = esExitosa ? AppTheme.accent : AppTheme.danger;
    final String titulo =
        esExitosa ? '¡Pago Aplicado con Éxito!' : 'Transacción Rechazada';

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
              // Logo del servicio y estado
              Center(
                child: Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    _iconoServicio(pago.servicio),
                    size: 40,
                    color: statusColor,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                titulo,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 6),
              Center(
                child: StatusBadge(estado: pago.estado ?? 'EXITOSA'),
              ),
              if (pago.mensajeGestoPago != null &&
                  pago.mensajeGestoPago!.trim().isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  '${pago.codigoGestoPago != null ? '[${pago.codigoGestoPago}] ' : ''}${pago.mensajeGestoPago}',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    color: esExitosa ? AppTheme.textSecondary : AppTheme.danger,
                  ),
                ),
              ],
              const SizedBox(height: 14),

              // Monto destacado
              Center(
                child: Text(
                  Formatters.formatCurrency(pago.monto),
                  style: TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.w900,
                    color: esExitosa ? AppTheme.primary : AppTheme.danger,
                  ),
                ),
              ),
              const SizedBox(height: 22),

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
                    _buildTicketRow('Referencia / Tel.', pago.referencia ?? '-'),
                    _buildTicketRow('Código UPC', pago.codigoUpc ?? 'N/A'),
                    _buildTicketRow(
                      'Folio / ID Tx',
                      pago.folioGestoPago ??
                          (pago.idTransaccion != null
                              ? 'TX-${pago.idTransaccion}'
                              : '-'),
                      isBold: true,
                    ),
                    _buildTicketRow(
                      'ID Tx Proveedor',
                      pago.idTransaccionProveedor ?? '-',
                    ),
                    _buildTicketRow(
                      'No. Autorización',
                      pago.autorizacionGestoPago ?? '-',
                    ),
                    _buildTicketRow(
                      'Cuenta Bancaria',
                      Formatters.maskAccount(pago.numeroCuenta),
                    ),
                    if (pago.saldoAnterior != null)
                      _buildTicketRow(
                        'Saldo Anterior',
                        Formatters.formatCurrency(pago.saldoAnterior),
                      ),
                    if (pago.saldoNuevo != null)
                      _buildTicketRow(
                        'Saldo Nuevo',
                        Formatters.formatCurrency(pago.saldoNuevo),
                        isBold: true,
                      ),
                    _buildTicketRow(
                      'Fecha y Hora',
                      Formatters.formatDateTime(
                        pago.fecha ?? DateTime.now().toIso8601String(),
                      ),
                    ),
                    _buildTicketRow(
                      'Clave Idempotencia',
                      pago.idempotencyKey ?? '-',
                    ),
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
