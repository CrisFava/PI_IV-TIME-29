import 'package:dio/dio.dart';

import 'problem_details.dart';

/// Extrai a [ProblemDetailsException] gerada pelo `ErrorInterceptor`.
extension ProblemDetailsX on DioException {
  ProblemDetailsException toProblem() {
    final problem = error;
    if (problem is ProblemDetailsException) return problem;
    final status = response?.statusCode;
    return status == null
        ? ProblemDetailsException.network()
        : ProblemDetailsException.fromStatus(status);
  }
}
