// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/app_config.dart';
import '../services/auth/auth_providers.dart';
import 'interceptors/auth_interceptor.dart';
import 'interceptors/error_interceptor.dart';
import 'interceptors/idempotency_interceptor.dart';

/// Cliente HTTP único da aplicação, já com os interceptors obrigatórios (RT11).
///
/// A URL base vem de `API_BASE_URL` no `.env`, os tempos de [AppConfig] e todo corpo vai e volta em
/// JSON. Os repositórios recebem este [Dio] pelo provider, o que permite trocá-lo nos testes. O cliente
/// é fechado quando o provider é descartado.
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: AppConfig.apiBaseUrl,
      connectTimeout: AppConfig.connectTimeout,
      receiveTimeout: AppConfig.receiveTimeout,
      contentType: Headers.jsonContentType,
      responseType: ResponseType.json,
    ),
  );

  // A ordem importa: o ErrorInterceptor fica por último para converter o
  // erro final, inclusive o de um retry feito pelo AuthInterceptor.
  dio.interceptors.addAll([
    AuthInterceptor(ref.watch(authServiceProvider), dio),
    IdempotencyInterceptor(),
    ErrorInterceptor(),
  ]);

  ref.onDispose(dio.close);
  return dio;
});
