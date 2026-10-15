import 'problem_details.dart';

/// Extrai o `data` do envelope `ApplicationResponse` do backend.
///
/// Respostas fora do envelope são devolvidas como estão, para tolerar
/// endpoints que respondam o objeto direto.
T unwrapApiResponse<T>(
  Object? body,
  T Function(Map<String, dynamic> json) fromJson,
) {
  if (body is! Map<String, dynamic>) {
    throw const ProblemDetailsException(
      title: 'Resposta inválida',
      detail: 'O servidor respondeu em um formato inesperado.',
      status: 500,
    );
  }

  if (body.containsKey('success') && body.containsKey('data')) {
    final data = body['data'];
    if (data is! Map<String, dynamic>) {
      throw const ProblemDetailsException(
        title: 'Resposta inválida',
        detail: 'O servidor respondeu sem os dados esperados.',
        status: 500,
      );
    }
    return fromJson(data);
  }

  return fromJson(body);
}
