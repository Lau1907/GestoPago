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
                    onPressed: () {
                      controller.text = AppConfig.defaultLoopbackUrl;
                    },
                    child: const Text('127.0.0.1:8080', style: TextStyle(fontSize: 11)),
                  ),
                  OutlinedButton(
                    onPressed: () {
                      controller.text = AppConfig.defaultAndroidEmulatorUrl;
                    },
                    child: const Text('10.0.2.2 (Emulador)', style: TextStyle(fontSize: 11)),
                  ),
                  OutlinedButton(
                    onPressed: () {
                      controller.text = AppConfig.defaultLocalhostUrl;
                    },
                    child: const Text('localhost:8080', style: TextStyle(fontSize: 11)),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              const Text(
                'Para dispositivo físico: use la IP local de su PC (ej. http://192.168.1.X:8080) o ejecute "adb reverse tcp:8080 tcp:8080".',
                style: TextStyle(fontSize: 10, color: Colors.grey, fontStyle: FontStyle.italic),
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
          '¿Está seguro de que desea cerrar su sesión? Se eliminarán los tokens de acceso y credenciales almacenadas de forma segura.',
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

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final configProvider = context.watch<ConfigProvider>();
    final cliente = authProvider.cliente;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Perfil y Configuración'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Tarjeta de Perfil adaptable
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 28,
                    backgroundColor: AppTheme.primary.withValues(alpha: 0.1),
                    child: const Icon(Icons.person, size: 34, color: AppTheme.primary),
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
                            fontWeight: FontWeight.bold,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          authProvider.currentEmail ?? 'usuario@correo.com',
                          style: const TextStyle(
                            fontSize: 13,
                            color: AppTheme.textSecondary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (cliente?.curp != null) ...[
                          const SizedBox(height: 2),
                          Text(
                            'CURP: ${cliente!.curp}',
                            style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
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
          const SizedBox(height: 12),

          // Sección de Cuentas Bancarias Asociadas
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4, vertical: 8),
            child: Text(
              'Cuentas Bancarias Disponibles',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppTheme.primary),
            ),
          ),
          if (authProvider.cuentas.isEmpty)
            const Card(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Text('No hay cuentas disponibles cargadas.'),
              ),
            )
          else
            ...authProvider.cuentas.map((cta) {
              final isSelected = authProvider.cuentaSeleccionada?.numeroCuenta == cta.numeroCuenta;
              return Card(
                color: isSelected ? Colors.blue.shade50 : Colors.white,
                child: ListTile(
                  leading: Icon(
                    Icons.account_balance,
                    color: isSelected ? AppTheme.primary : Colors.grey,
                  ),
                  title: Text(
                    Formatters.maskAccount(cta.numeroCuenta),
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  subtitle: Text('Estatus: ${cta.estatus}'),
                  trailing: Text(
                    Formatters.formatCurrency(cta.saldo),
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                      color: AppTheme.accent,
                    ),
                  ),
                  onTap: () => authProvider.seleccionarCuenta(cta),
                ),
              );
            }),

          const SizedBox(height: 16),

          // Sección de Conectividad Backend
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4, vertical: 8),
            child: Text(
              'Conectividad del Backend',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppTheme.primary),
            ),
          ),
          Card(
            child: ListTile(
              leading: const Icon(Icons.dns_outlined, color: AppTheme.primary),
              title: const Text('URL del Servidor Spring Boot'),
              subtitle: Text(
                configProvider.currentBaseUrl,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              trailing: const Icon(Icons.edit_outlined),
              onTap: () => _dialogoCambiarUrl(context),
            ),
          ),
          const Card(
            child: ListTile(
              leading: Icon(Icons.phone_android, color: AppTheme.primary),
              title: Text('Canal Registrado'),
              subtitle: Text('MOBILE (Control de Sesión Única activo)'),
              trailing: Icon(Icons.check_circle, color: AppTheme.accent, size: 20),
            ),
          ),

          const SizedBox(height: 28),

          // Botón Cerrar Sesión
          CustomButton(
            text: 'Cerrar Sesión Segura',
            icon: Icons.logout,
            color: AppTheme.danger,
            isOutlined: true,
            onPressed: () => _onCerrarSesion(context),
          ),
        ],
      ),
    );
  }
}
