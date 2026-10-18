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
      final token = await _authService.getIdToken();
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
}
