import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../config/app_config.dart';
import '../../../config/app_theme.dart';
import '../../../core/utils/formatters.dart';
import '../../../providers/auth_provider.dart';
import '../../../providers/config_provider.dart';
import '../../../providers/historial_provider.dart';
import '../../widgets/custom_button.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  void _dialogoCambiarUrl(BuildContext context) {
    final configProvider = context.read<ConfigProvider>();
    final controller = TextEditingController(text: configProvider.currentBaseUrl);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Configuración Base URL'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Seleccione o ingrese la URL de su backend Spring Boot:',
                style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: controller,
                style: const TextStyle(color: AppTheme.textPrimary),
                decoration: const InputDecoration(
                  labelText: 'Base URL Backend',
                  hintText: 'http://127.0.0.1:8080',
                ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  OutlinedButton(
                    onPressed: () => controller.text = AppConfig.defaultLoopbackUrl,
                    child: const Text('127.0.0.1:8080', style: TextStyle(fontSize: 11)),
                  ),
                  OutlinedButton(
                    onPressed: () => controller.text = AppConfig.defaultAndroidEmulatorUrl,
                    child: const Text('10.0.2.2 (Emulador)', style: TextStyle(fontSize: 11)),
                  ),
                  OutlinedButton(
                    onPressed: () => controller.text = AppConfig.defaultLocalhostUrl,
                    child: const Text('localhost:8080', style: TextStyle(fontSize: 11)),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              const Text(
                'Para dispositivo físico: use la IP local de su PC o ejecute "adb reverse tcp:8080 tcp:8080".',
                style: TextStyle(fontSize: 10, color: AppTheme.textMuted, fontStyle: FontStyle.italic),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () {
              configProvider.updateBaseUrl(controller.text.trim());
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('URL actualizada a: ${controller.text.trim()}')),
              );
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
  }

  void _onCerrarSesion(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar Sesión Segura'),
        content: const Text(
          '¿Está seguro de que desea cerrar su sesión? Se eliminarán los tokens de acceso y credenciales almacenadas.',
          style: TextStyle(fontSize: 14),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.danger),
            onPressed: () async {
              Navigator.pop(ctx);
              final authProvider = context.read<AuthProvider>();
              final historialProvider = context.read<HistorialProvider>();
              await authProvider.logout();
              await historialProvider.limpiar();
            },
            child: const Text('Cerrar Sesión'),
          ),
        ],
      ),
    );
  }

  Widget _sectionTitle(String text) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, top: 16, bottom: 8),
      child: Text(
        text,
        style: const TextStyle(
          fontWeight: FontWeight.w700,
          fontSize: 13,
          color: AppTheme.primaryLight,
          letterSpacing: 0.5,
        ),
      ),
    );
  }

  Widget _darkCard({required Widget child}) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.cardBg,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppTheme.borderColor),
      ),
      child: child,
    );
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final configProvider = context.watch<ConfigProvider>();
    final cliente = authProvider.cliente;

    return Scaffold(
      backgroundColor: AppTheme.surface,
      appBar: AppBar(title: const Text('Perfil y Ajustes')),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        children: [
          // ── Tarjeta de Perfil ──────────────────────────────────────────────
          _darkCard(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Container(
                    width: 54,
                    height: 54,
                    decoration: BoxDecoration(
                      color: AppTheme.primary.withValues(alpha: 0.15),
                      shape: BoxShape.circle,
                      border: Border.all(color: AppTheme.primary.withValues(alpha: 0.3)),
                    ),
                    child: const Icon(Icons.person, size: 30, color: AppTheme.primaryLight),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          cliente?.nombreCompleto ?? 'Cliente Titular',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            color: AppTheme.textPrimary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          authProvider.currentEmail ?? 'usuario@correo.com',
                          style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (cliente?.curp != null) ...[
                          const SizedBox(height: 2),
                          Text(
                            'CURP: ${cliente!.curp}',
                            style: const TextStyle(fontSize: 11, color: AppTheme.textMuted),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),

          // ── Cuentas bancarias ──────────────────────────────────────────────
          _sectionTitle('Cuentas Bancarias Disponibles'),
          if (authProvider.cuentas.isEmpty)
            _darkCard(
              child: const Padding(
                padding: EdgeInsets.all(16),
                child: Text(
                  'No hay cuentas disponibles.',
                  style: TextStyle(color: AppTheme.textSecondary),
                ),
              ),
            )
          else
            ...authProvider.cuentas.map((cta) {
              final isSelected =
                  authProvider.cuentaSeleccionada?.numeroCuenta == cta.numeroCuenta;
              return Container(
                margin: const EdgeInsets.only(bottom: 8),
                decoration: BoxDecoration(
                  color: isSelected
                      ? AppTheme.primary.withValues(alpha: 0.14)
                      : AppTheme.cardBg,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: isSelected ? AppTheme.primary.withValues(alpha: 0.5) : AppTheme.borderColor,
                    width: isSelected ? 1.5 : 1,
                  ),
                ),
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  leading: Icon(
                    Icons.account_balance,
                    color: isSelected ? AppTheme.primaryLight : AppTheme.textSecondary,
                  ),
                  title: Text(
                    Formatters.maskAccount(cta.numeroCuenta),
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      color: isSelected ? AppTheme.textPrimary : AppTheme.textPrimary,
                    ),
                  ),
                  subtitle: Text('Estatus: ${cta.estatus}'),
                  trailing: Text(
                    Formatters.formatCurrency(cta.saldo),
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 15,
                      color: AppTheme.accent,
                    ),
                  ),
                  onTap: () => authProvider.seleccionarCuenta(cta),
                ),
              );
            }),

          // ── Conectividad ───────────────────────────────────────────────────
          _sectionTitle('Conectividad del Backend'),
          _darkCard(
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              leading: const Icon(Icons.dns_outlined, color: AppTheme.primaryLight),
              title: const Text('URL del Servidor Spring Boot',
                  style: TextStyle(color: AppTheme.textPrimary)),
              subtitle: Text(
                configProvider.currentBaseUrl,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              trailing: const Icon(Icons.edit_outlined, color: AppTheme.textSecondary, size: 20),
              onTap: () => _dialogoCambiarUrl(context),
            ),
          ),
          const SizedBox(height: 8),
          _darkCard(
            child: const ListTile(
              contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              leading: Icon(Icons.phone_android, color: AppTheme.primaryLight),
              title: Text('Canal Registrado', style: TextStyle(color: AppTheme.textPrimary)),
              subtitle: Text('MOBILE (Control de Sesión Única activo)'),
              trailing: Icon(Icons.check_circle, color: AppTheme.accent, size: 20),
            ),
          ),

          const SizedBox(height: 28),

          // ── Cerrar Sesión ──────────────────────────────────────────────────
          CustomButton(
            text: 'Cerrar Sesión Segura',
            icon: Icons.logout,
            color: AppTheme.danger,
            isOutlined: true,
            onPressed: () => _onCerrarSesion(context),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
