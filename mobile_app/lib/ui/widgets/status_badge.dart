import 'package:flutter/material.dart';
import '../../config/app_theme.dart';

/// Badge de estado con paleta oscura fintech
class StatusBadge extends StatelessWidget {
  final String estado;

  const StatusBadge({super.key, required this.estado});

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    Color borderCol;
    IconData icon;
    String label = estado;

    switch (estado.toUpperCase()) {
      case 'EXITOSA':
      case 'APROBADA':
        bg = AppTheme.accentLight;
        fg = const Color(0xFF34D399);
        borderCol = const Color(0x5910B981);
        icon = Icons.check_circle_outline;
        label = 'EXITOSA';
        break;
      case 'TIMEOUT_REVISION':
      case 'INICIADA':
      case 'PENDIENTE':
        bg = AppTheme.warningLight;
        fg = const Color(0xFFFCD34D);
        borderCol = const Color(0x59F59E0B);
        icon = Icons.access_time;
        label = 'EN REVISIÓN';
        break;
      case 'FALLIDA':
      case 'RECHAZADA':
      default:
        bg = AppTheme.dangerLight;
        fg = const Color(0xFFF87171);
        borderCol = const Color(0x59EF4444);
        icon = Icons.cancel_outlined;
        label = 'RECHAZADA';
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: borderCol, width: 1),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: fg),
          const SizedBox(width: 5),
          Text(
            label,
            style: TextStyle(
              color: fg,
              fontSize: 11,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.4,
            ),
          ),
        ],
      ),
    );
  }
}
