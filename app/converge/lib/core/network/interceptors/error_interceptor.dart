import 'package:dio/dio.dart';

import '../problem_details.dart';

/// Converte qualquer falha HTTP em [ProblemDetailsException], disponível em
/// `DioException.error`. Use [ProblemDetailsX.toProblem] para extrair.
class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    handler.next(err.copyWith(error: _toProblem(err)));
  }

  ProblemDetailsException _toProblem(DioException err) {
    final existing = err.error;
    if (existing is ProblemDetailsException) return existing;

    final response = err.response;
    if (response == null) return ProblemDetailsException.network();

    final status = response.statusCode ?? 500;
    final body = response.data;
    if (body is Map<String, dynamic>) {
      return ProblemDetailsException.fromJson(body, status: status);
    }
    return ProblemDetailsException.fromStatus(status);
  }
}
