import 'package:dio/dio.dart';

import 'api_exception.dart';

/// Extrai a [ApiException] gerada pelo `ErrorInterceptor`.
extension ApiExceptionX on DioException {
  ApiException toApiException() {
    final apiError = error;
    if (apiError is ApiException) return apiError;
    final status = response?.statusCode;
    return status == null
        ? ApiException.network()
        : ApiException.fromResponse(response?.data, status: status);
  }
}
