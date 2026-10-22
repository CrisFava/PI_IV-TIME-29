import 'dart:convert';
import 'package:dio/dio.dart';

import '../../services/auth/auth_service.dart';

/// Injeta `Authorization: Bearer <Firebase ID token>` e, se o backend
/// responder 401, força a renovação do token e repete a requisição uma vez.
class AuthInterceptor extends Interceptor {
  static const String _retriedFlag = 'auth_retried';

  final AuthService _authService;
  final Dio _dio;

  AuthInterceptor(this._authService, this._dio);

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    try {
      // Se a requisição já é um retry com Authorization configurado, não sobrescreve
      if (options.extra[_retriedFlag] == true &&
          options.headers.containsKey('Authorization')) {
        return handler.next(options);
      }

      var token = await _authService.getIdToken();

      // Se o token existir e já estiver expirado ou perto de expirar (< 60s),
      // força a renovação antes de enviar ao backend para evitar rejeição com 401.
      if (token != null && _isTokenExpiredOrExpiring(token)) {
        token = await _authService.getIdToken(forceRefresh: true);
      }

      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    } catch (e) {
      handler.reject(DioException(requestOptions: options, error: e));
    }
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final options = err.requestOptions;
    final alreadyRetried = options.extra[_retriedFlag] == true;
    if (err.response?.statusCode != 401 || alreadyRetried) {
      return handler.next(err);
    }

    try {
      final token = await _authService.getIdToken(forceRefresh: true);
      if (token == null) return handler.next(err);

      options.extra[_retriedFlag] = true;
      options.headers['Authorization'] = 'Bearer $token';
      handler.resolve(await _dio.fetch<dynamic>(options));
    } on DioException catch (retryError) {
      handler.next(retryError);
    } catch (_) {
      handler.next(err);
    }
  }

  /// Verifica se o token JWT está expirado ou se expira em menos de 60 segundos.
  /// Para tokens de teste/fakes (sem 3 partes separadas por ponto), retorna false.
  bool _isTokenExpiredOrExpiring(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return false;
      final normalized = base64Url.normalize(parts[1]);
      final payloadString = utf8.decode(base64Url.decode(normalized));
      final payload = jsonDecode(payloadString);
      if (payload is Map<String, dynamic> && payload.containsKey('exp')) {
        final exp = payload['exp'];
        if (exp is num) {
          final nowInSeconds =
              DateTime.now().toUtc().millisecondsSinceEpoch ~/ 1000;
          return nowInSeconds >= (exp.toInt() - 60);
        }
      }
      return false;
    } catch (_) {
      return false;
    }
  }
}
