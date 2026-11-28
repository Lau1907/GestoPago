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
    if (s.contains('luz') || s.contains('cfe') || s.contains('energia')) return Icons.bolt_outlined;
    if (s.contains('gas')) return Icons.local_fire_department_outlined;
    if (s.contains('tel') || s.contains('movil') || s.contains('recarga')) return Icons.smartphone_outlined;
    if (s.contains('tv') || s.contains('cable') || s.contains('sky')) return Icons.tv_outlined;
    if (s.contains('internet')) return Icons.wifi_outlined;
    return Icons.receipt_long_outlined;
  }

  Widget _ticketRow(String label, String value, {bool isBold = false, Color? valueColor}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 130,
            child: Text(
              label,
              style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
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
                fontSize: 12,
                fontWeight: isBold ? FontWeight.w800 : FontWeight.w600,
                color: valueColor ?? AppTheme.textPrimary,
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
    final String titulo = esExitosa ? '¡Pago Aplicado con Éxito!' : 'Transacción Rechazada';

    return Scaffold(
      backgroundColor: AppTheme.surface,
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
              // ── Logo del servicio ────────────────────────────────────────
              Center(
                child: Container(
                  width: 76,
                  height: 76,
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: 0.14),
                    shape: BoxShape.circle,
                    border: Border.all(color: statusColor.withValues(alpha: 0.35)),
                  ),
                  child: ClipOval(
                    child: pago.urlLogo != null
                        ? Image.network(
                            pago.urlLogo!,
                            width: 76,
                            height: 76,
                            fit: BoxFit.contain,
                            errorBuilder: (_, __, ___) =>
                                Icon(_iconoServicio(pago.servicio), size: 40, color: statusColor),
                          )
                        : Icon(_iconoServicio(pago.servicio), size: 40, color: statusColor),
                  ),
                ),
              ),
              const SizedBox(height: 14),

              // ── Título ────────────────────────────────────────────────────
              Text(
                titulo,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 8),

              // ── Badge de estado ───────────────────────────────────────────
              Center(child: StatusBadge(estado: pago.estado ?? 'EXITOSA')),

              if (pago.mensajeGestoPago != null && pago.mensajeGestoPago!.trim().isNotEmpty) ...[
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
              const SizedBox(height: 16),

              // ── Monto destacado ───────────────────────────────────────────
              Center(
                child: Text(
                  Formatters.formatCurrency(pago.monto),
                  style: TextStyle(
                    fontSize: 36,
                    fontWeight: FontWeight.w900,
                    color: esExitosa ? AppTheme.accent : AppTheme.danger,
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // ── Ticket bancario oscuro ────────────────────────────────────
              Container(
                decoration: BoxDecoration(
                  color: AppTheme.cardBg,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppTheme.borderColor),
                ),
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Encabezado del ticket
                    const Row(
                      children: [
                        Icon(Icons.receipt_long, size: 18, color: AppTheme.primaryLight),
                        SizedBox(width: 8),
                        Text(
                          'Detalle de la Operación GestoPago',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                            color: AppTheme.primaryLight,
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 20, color: AppTheme.borderColor),

                    _ticketRow('SERVICIO', pago.servicio ?? 'GestoPago'),
                    _ticketRow('PRODUCTO', pago.producto ?? '-'),
                    _ticketRow('REFERENCIA / TEL.', pago.referencia ?? '-'),
                    _ticketRow('CÓDIGO UPC', pago.codigoUpc ?? 'N/A'),
                    _ticketRow(
                      'FOLIO / ID TX',
                      pago.folioGestoPago ??
                          (pago.idTransaccion != null ? 'TX-${pago.idTransaccion}' : '-'),
                      isBold: true,
                      valueColor: AppTheme.textPrimary,
                    ),
                    _ticketRow('ID TX PROVEEDOR', pago.idTransaccionProveedor ?? '-'),
                    _ticketRow('AUTORIZACIÓN', pago.autorizacionGestoPago ?? '-'),
                    _ticketRow('CUENTA BANCARIA', Formatters.maskAccount(pago.numeroCuenta)),

                    const Divider(height: 20, color: AppTheme.borderColor),

                    _ticketRow(
                      'MONTO',
                      Formatters.formatCurrency(pago.monto),
                      isBold: true,
                      valueColor: esExitosa ? AppTheme.accent : AppTheme.danger,
                    ),
                    if (pago.saldoAnterior != null)
                      _ticketRow('SALDO ANTERIOR', Formatters.formatCurrency(pago.saldoAnterior)),
                    if (pago.saldoNuevo != null)
                      _ticketRow(
                        'SALDO NUEVO',
                        Formatters.formatCurrency(pago.saldoNuevo),
                        isBold: true,
                        valueColor: AppTheme.accent,
                      ),

                    const Divider(height: 20, color: AppTheme.borderColor),

                    _ticketRow('FECHA/HORA', Formatters.formatDateTime(pago.fecha ?? DateTime.now().toIso8601String())),
                    _ticketRow('IDEMPOTENCY KEY', pago.idempotencyKey ?? '-'),

                    if (pago.ticket != null && pago.ticket!.isNotEmpty) ...[
                      const SizedBox(height: 14),
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppTheme.inputBg,
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: AppTheme.borderSubtle),
                        ),
                        child: Text(
                          'Ticket / Leyenda:\n${pago.ticket}',
                          style: const TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 11,
                            color: AppTheme.textSecondary,
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
                text: 'Aceptar y Finalizar',
                icon: Icons.home_outlined,
                onPressed: () => Navigator.popUntil(context, (route) => route.isFirst),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
