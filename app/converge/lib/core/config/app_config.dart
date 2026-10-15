abstract class AppConfig {
  static const String appName = 'Converge';

  /// Sobrescreva com `--dart-define=API_BASE_URL=...`.
  /// No emulador Android, use `http://10.0.2.2:8080/api/v1` para alcançar o host.
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api/v1',
  );

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);
}
