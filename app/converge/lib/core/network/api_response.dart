import 'api_exception.dart';

/// Extrai o `data` do envelope `ApplicationResponse` do backend:
/// `{ success, data, error, timestamp }`.
///
/// Lança [ApiException] se `success` for `false` ou se o corpo não estiver
/// no formato esperado.
T unwrapApiResponse<T>(
  Object? body,
  T Function(Map<String, dynamic> json) fromJson, {
  int status = 200,
}) {
  if (body is! Map<String, dynamic> || body['success'] is! bool) {
    throw ApiException.fromStatus(500);
  }

  if (body['success'] != true) {
    throw ApiException.fromResponse(body, status: status);
  }

  final data = body['data'];
  if (data is! Map<String, dynamic>) throw ApiException.fromStatus(500);
  return fromJson(data);
}
