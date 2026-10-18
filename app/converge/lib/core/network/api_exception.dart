import 'api_error_code.dart';

/// Erro de API no formato normalizado pelo backend:
/// `ApplicationResponse { success: false, error: { code, message, fields } }`,
/// acrescido do status HTTP da resposta.
class ApiException implements Exception {
  final ApiErrorCode code;
  final String message;
  final Set<String> fields;
  final int status;

  const ApiException({
    required this.code,
    required this.message,
    required this.status,
    this.fields = const {},
  });

  /// Status usado quando não houve resposta do servidor.
  static const int networkStatus = 0;

  bool get isNetworkError => code == ApiErrorCode.network;
  bool get isNotFound => status == 404;
  bool get isConflict => status == 409;
  bool get isUnauthorized => status == 401;

  factory ApiException.network() => const ApiException(
    code: ApiErrorCode.network,
    message: 'Não foi possível conectar ao servidor. Verifique sua internet.',
    status: networkStatus,
  );

  /// Erro sem `error` no corpo (ex.: rota inexistente, falha fora do handler).
  factory ApiException.fromStatus(int status) => ApiException(
    code: ApiErrorCode.unknown,
    message: _defaultMessage(status),
    status: status,
  );

  /// Lê o `error` do envelope `ApplicationResponse`; sem ele, cai em
  /// [ApiException.fromStatus].
  factory ApiException.fromResponse(Object? body, {required int status}) {
    final error = body is Map<String, dynamic> ? body['error'] : null;
    if (error is! Map<String, dynamic>) return ApiException.fromStatus(status);

    final fields = error['fields'];
    return ApiException(
      code: ApiErrorCode.fromValue(error['code']),
      message: error['message'] as String? ?? _defaultMessage(status),
      status: status,
      fields: fields is List
          ? fields.map((f) => f.toString()).toSet()
          : const {},
    );
  }

  static String _defaultMessage(int status) => switch (status) {
    401 => 'Sua sessão expirou. Entre novamente.',
    403 => 'Você não tem permissão para esta ação.',
    404 => 'Recurso não encontrado.',
    409 => 'O recurso já existe.',
    >= 500 => 'O servidor encontrou um erro. Tente novamente mais tarde.',
    _ => 'Ocorreu um erro ao processar sua solicitação.',
  };

  @override
  String toString() => 'ApiException($status, ${code.value}): $message';
}
