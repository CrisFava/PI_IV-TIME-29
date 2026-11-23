// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';

import '../../services/auth/auth_exception.dart';
import '../api_exception.dart';

/// Converte qualquer falha HTTP em [ApiException], disponível em
/// `DioException.error`. Use [ApiExceptionX.toApiException] para extrair.
class ErrorInterceptor extends Interceptor {
  /// Troca o `error` do [DioException] por uma [ApiException], mantendo o resto (requisição e resposta).
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    // Falhas de identidade (ex.: sessão trocada) seguem como AuthException.
    if (err.error is AuthException) return handler.next(err);
    handler.next(err.copyWith(error: _toApiException(err)));
  }

  /// Reaproveita uma [ApiException] já existente; sem resposta, é erro de rede; com resposta, lê o
  /// envelope `ApplicationResponse` do corpo.
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
