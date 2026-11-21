import 'package:flutter/foundation.dart';
import '../../config/app_config.dart';

class ConfigProvider extends ChangeNotifier {
  String _currentBaseUrl = AppConfig.baseUrl;

  String get currentBaseUrl => _currentBaseUrl;

  Future<void> updateBaseUrl(String newUrl) async {
    await AppConfig.setBaseUrl(newUrl);
    _currentBaseUrl = AppConfig.baseUrl;
    notifyListeners();
  }

  void setAndroidEmulatorUrl() {
    updateBaseUrl(AppConfig.defaultAndroidEmulatorUrl);
  }

  void setLoopbackUrl() {
    updateBaseUrl(AppConfig.defaultLoopbackUrl);
  }

  void setLocalhostUrl() {
    updateBaseUrl(AppConfig.defaultLocalhostUrl);
  }
}
