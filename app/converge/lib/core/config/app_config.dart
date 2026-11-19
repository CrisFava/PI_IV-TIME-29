// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_dotenv/flutter_dotenv.dart';

abstract class AppConfig {
  static const String appName = 'Converge';

  /// URL da API Spring Boot, lida de `API_BASE_URL` no `.env` do app (veja o
  /// `.env.example`). Sem a variável, o app avisa em vez de usar um endereço fixo.
  static String get apiBaseUrl {
    final url = dotenv.isInitialized ? dotenv.maybeGet('API_BASE_URL') : null;
    if (url == null || url.trim().isEmpty) {
      throw StateError(
        'API_BASE_URL não definida: adicione a URL da API no app/converge/.env '
        '(veja o .env.example).',
      );
    }
    return url.trim();
  }

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);
}
