import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../config/app_theme.dart';
import '../../../core/utils/formatters.dart';
import '../../../models/gestopago/pago_response.dart';
import '../../../providers/auth_provider.dart';
import '../../../providers/historial_provider.dart';
import '../../widgets/empty_state_view.dart';
import '../../widgets/status_badge.dart';
import '../pago/comprobante_pago_screen.dart';

class HistorialScreen extends StatefulWidget {
  const HistorialScreen({super.key});

  @override
  State<HistorialScreen> createState() => _HistorialScreenState();
}

class _HistorialScreenState extends State<HistorialScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _recargar();
    });
  }

  void _recargar() {
    final auth = context.read<AuthProvider>();
    context.read<HistorialProvider>().cargarHistorial(
      clienteId: auth.clienteId,
      numeroCuenta: auth.cuentaSeleccionada?.numeroCuenta,
    );
  }

  void _verComprobante(PagoServicioResponse pago) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ComprobantePagoScreen(pago: pago),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final historialProvider = context.watch<HistorialProvider>();
    final listaExitosas = historialProvider.comprasExitosas;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Historial de Compras'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar',
            onPressed: _recargar,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async => _recargar(),
        child: Builder(
          builder: (context) {
            if (historialProvider.isLoading && listaExitosas.isEmpty) {
              return const Center(child: CircularProgressIndicator());
            }

            if (listaExitosas.isEmpty) {
              return EmptyStateView(
                icon: Icons.receipt_long_outlined,
                title: 'Sin compras exitosas',
                message:
                    'Aquí se mostrarán únicamente las compras y pagos completados exitosamente en la aplicación.',
                actionText: 'Actualizar',
                onAction: _recargar,
              );
            }

            return ListView.separated(
              padding: const EdgeInsets.symmetric(vertical: 12),
              itemCount: listaExitosas.length,
              separatorBuilder: (_, __) => const SizedBox(height: 2),
              itemBuilder: (context, index) {
                final pago = listaExitosas[index];

                return Card(
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () => _verComprobante(pago),
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Cabecera de la tarjeta: Producto y Estado
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  pago.producto ?? 'Servicio Pagado',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 15,
                                    color: AppTheme.textPrimary,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const SizedBox(width: 8),
                              const StatusBadge(estado: 'EXITOSA'),
                            ],
                          ),
                          const SizedBox(height: 4),

                          // Servicio
                          Text(
                            pago.servicio ?? 'GestoPago',
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const Divider(height: 18),

                          // Datos de la compra adaptables: Importe, Fecha, Folio y Referencia
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: [
                              // Columna izquierda adaptable (Folio, Ref, Fecha)
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Folio: ${pago.folioGestoPago ?? 'GP-${pago.idTransaccion ?? 'N/A'}'}',
                                      style: const TextStyle(
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                        color: AppTheme.primary,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      'Ref: ${pago.referencia ?? '-'}',
                                      style: TextStyle(
                                        fontSize: 11,
                                        color: Colors.grey.shade600,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      Formatters.formatDateTime(pago.fecha),
                                      style: TextStyle(
                                        fontSize: 11,
                                        color: Colors.grey.shade500,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 12),
                              // Columna derecha fija/flexible (Monto y enlace Ver Ticket)
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(
                                    Formatters.formatCurrency(pago.monto),
                                    style: const TextStyle(
                                      fontSize: 17,
                                      fontWeight: FontWeight.w900,
                                      color: AppTheme.accent,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  const Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Text(
                                        'Ver Ticket',
                                        style: TextStyle(
                                          fontSize: 11,
                                          color: AppTheme.primary,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                      Icon(Icons.chevron_right, size: 16, color: AppTheme.primary),
                                    ],
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }
}
