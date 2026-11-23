// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';

import 'api_exception.dart';

/// Extrai a [ApiException] gerada pelo `ErrorInterceptor`.
extension ApiExceptionX on DioException {
  /// Devolve a [ApiException] que o interceptor já colocou em `error`. Se o erro não passou pelo
  /// interceptor (ex.: Dio montado sem ele nos testes), monta a exceção a partir da resposta, ou de
  /// rede quando não houve resposta.
  ApiException toApiException() {
    final apiError = error;
    if (apiError is ApiException) return apiError;
    final status = response?.statusCode;
    return status == null
        ? ApiException.network()
        : ApiException.fromResponse(response?.data, status: status);
  }
}
