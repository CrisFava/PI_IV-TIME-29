import 'package:dio/dio.dart';

import '../api_exception.dart';

/// Converte qualquer falha HTTP em [ApiException], disponível em
/// `DioException.error`. Use [ApiExceptionX.toApiException] para extrair.
class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    handler.next(err.copyWith(error: _toApiException(err)));
  }

  ApiException _toApiException(DioException err) {
    final existing = err.error;
    if (existing is ApiException) return existing;

    final response = err.response;
    if (response == null) return ApiException.network();

    return ApiException.fromResponse(
      response.data,
      status: response.statusCode ?? 500,
    );
  }
}
