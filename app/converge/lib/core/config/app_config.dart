abstract class AppConfig {
  static const String appName = 'Converge';

  /// Conecta estritamente ao `http://localhost:8080/api/v1` por padrão.
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api/v1',
  );

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);
}
