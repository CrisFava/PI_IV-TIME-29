// Autor: Cristian Eduardo Fava - RA 25000636

import 'api_exception.dart';

/// Extrai o `data` do envelope `ApplicationResponse` do backend:
/// `{ success, data, error, timestamp }`.
///
/// Lança [ApiException] se `success` for `false` ou se o corpo não estiver
/// no formato esperado.
///
/// - `body`: o corpo já decodificado da resposta do Dio.
/// - `fromJson`: conversor do `data` para o modelo.
/// - `status`: status HTTP, repassado para a exceção quando `success` é `false`.
///
/// Um corpo fora do envelope ou um `data` que não é objeto é tratado como erro 500.
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

/// Extrai uma lista de itens do campo `data` no envelope `ApplicationResponse`.
///
/// Mesmas regras de [unwrapApiResponse], mas `data` precisa ser uma lista. Itens que não são objetos
/// JSON são descartados em vez de derrubar a lista inteira.
List<T> unwrapApiListResponse<T>(
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
  if (data is! List<dynamic>) throw ApiException.fromStatus(500);
  return data.whereType<Map<String, dynamic>>().map(fromJson).toList();
}
