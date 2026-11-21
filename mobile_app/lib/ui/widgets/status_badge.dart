import 'package:flutter/material.dart';
import '../../config/app_theme.dart';

class StatusBadge extends StatelessWidget {
  final String estado;

  const StatusBadge({super.key, required this.estado});

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    IconData icon;
    String label = estado;

    switch (estado.toUpperCase()) {
      case 'EXITOSA':
      case 'APROBADA':
        bg = AppTheme.accent.withValues(alpha: 0.12);
        fg = AppTheme.accent;
        icon = Icons.check_circle_outline;
        label = 'Exitosa';
        break;
      case 'TIMEOUT_REVISION':
      case 'INICIADA':
      case 'PENDIENTE':
        bg = AppTheme.warning.withValues(alpha: 0.12);
        fg = AppTheme.warning;
        icon = Icons.access_time;
        label = 'En Revisión';
        break;
      case 'FALLIDA':
      case 'RECHAZADA':
      default:
        bg = AppTheme.danger.withValues(alpha: 0.12);
        fg = AppTheme.danger;
        icon = Icons.cancel_outlined;
        label = 'Rechazada';
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: fg),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              color: fg,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
