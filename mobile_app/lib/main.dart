import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';
import 'config/app_config.dart';
import 'config/app_theme.dart';
import 'core/api/api_client.dart';
import 'providers/auth_provider.dart';
import 'providers/catalogo_provider.dart';
import 'providers/config_provider.dart';
import 'providers/historial_provider.dart';
import 'providers/pago_provider.dart';
import 'ui/screens/auth/login_screen.dart';
import 'ui/screens/home/main_navigation_screen.dart';

// Llave global para navegación y alertas globales de 401 Sesión Invalidada
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppConfig.initialize();

  runApp(const BancaMovilApp());
}

class BancaMovilApp extends StatefulWidget {
  const BancaMovilApp({super.key});

  @override
  State<BancaMovilApp> createState() => _BancaMovilAppState();
}

class _BancaMovilAppState extends State<BancaMovilApp> {
  @override
  void initState() {
    super.initState();
    _configurarInterceptorSesionUnica();
  }

  void _configurarInterceptorSesionUnica() {
    // Interceptor global cuando el ApiClient capture HTTP 401
    // (sesión cerrada por inicio en otro dispositivo como "WEB")
    ApiClient.onSessionInvalidated = (mensaje) async {
      final nav = navigatorKey.currentState;
      final context = navigatorKey.currentContext;

      if (context != null) {
        // 1. Limpiar estado en AuthProvider y FlutterSecureStorage
        final authProvider = context.read<AuthProvider>();
        await authProvider.handleSessionInvalidated(mensaje);
      }

      // 2. Limpiar completamente la pila de rutas (cerrando cualquier diálogo, modal o bottom sheet)
      //    y posicionar al usuario en el AuthWrapper para que al completar Login+2FA muestre el Home
      nav?.pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const AuthWrapper()),
        (route) => false,
      );
    };
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => ConfigProvider()),
        ChangeNotifierProvider(create: (_) => AuthProvider()..checkAuthStatus()),
        ChangeNotifierProvider(create: (_) => CatalogoProvider()),
        ChangeNotifierProvider(create: (_) => PagoProvider()),
        ChangeNotifierProvider(create: (_) => HistorialProvider()),
      ],
      child: MaterialApp(
        title: 'Banca Móvil GestoPago',
        navigatorKey: navigatorKey,
        debugShowCheckedModeBanner: false,
        theme: AppTheme.darkTheme,
        darkTheme: AppTheme.darkTheme,
        themeMode: ThemeMode.dark,
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: const [
          Locale('es', 'MX'),
          Locale('en', 'US'),
        ],
        home: const AuthWrapper(),
      ),
    );
  }
}

class AuthWrapper extends StatelessWidget {
  const AuthWrapper({super.key});

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();

    switch (authProvider.status) {
      case AuthStatus.authenticated:
        return const MainNavigationScreen();
      case AuthStatus.authenticating:
        return const Scaffold(
          body: Center(child: CircularProgressIndicator()),
        );
      case AuthStatus.requiring2FA:
      case AuthStatus.unauthenticated:
        return const LoginScreen();
    }
  }
}
