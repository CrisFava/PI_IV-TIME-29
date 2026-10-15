/// Erro de API padronizado e legível para a interface.
///
/// Aceita os dois formatos que o backend pode devolver:
/// - o envelope `ApplicationResponse` atual do Spring Boot
///   (`{ success, data, error: { code, message, fields }, timestamp }`);
/// - RFC 7807 Problem Details (`{ title, detail, status, code, errors }`).
class ProblemDetailsException implements Exception {
  final String title;
  final String detail;
  final int status;
  final String? code;
  final Map<String, dynamic>? errors;

  const ProblemDetailsException({
    required this.title,
    required this.detail,
    required this.status,
    this.code,
    this.errors,
  });

  /// Sem resposta do servidor (timeout, sem rede, servidor fora do ar).
  static const int networkStatus = 0;

  bool get isNetworkError => status == networkStatus;
  bool get isNotFound => status == 404;
  bool get isUnauthorized => status == 401;

  factory ProblemDetailsException.network() => const ProblemDetailsException(
    title: 'Sem conexão',
    detail: 'Não foi possível conectar ao servidor. Verifique sua internet.',
    status: networkStatus,
  );

  factory ProblemDetailsException.fromStatus(int status) =>
      ProblemDetailsException(
        title: 'Erro inesperado',
        detail: switch (status) {
          401 => 'Sua sessão expirou. Entre novamente.',
          403 => 'Você não tem permissão para esta ação.',
          404 => 'Recurso não encontrado.',
          >= 500 => 'O servidor encontrou um erro. Tente novamente mais tarde.',
          _ => 'Ocorreu um erro ao processar sua solicitação.',
        },
        status: status,
      );

  factory ProblemDetailsException.fromJson(
    Map<String, dynamic> json, {
    required int status,
  }) {
    final envelopeError = json['error'];
    if (envelopeError is Map<String, dynamic>) {
      final fields = envelopeError['fields'];
      return ProblemDetailsException(
        title: 'Erro na requisição',
        detail:
            envelopeError['message'] as String? ??
            ProblemDetailsException.fromStatus(status).detail,
        status: status,
        code: envelopeError['code'] as String?,
        errors: fields is List && fields.isNotEmpty
            ? {for (final field in fields) field.toString(): null}
            : null,
      );
    }

    if (json.containsKey('title') || json.containsKey('detail')) {
      return ProblemDetailsException(
        title: json['title'] as String? ?? 'Erro inesperado',
        detail:
            json['detail'] as String? ??
            'Ocorreu um erro ao processar sua solicitação.',
        status: json['status'] as int? ?? status,
        code: json['code'] as String?,
        errors: json['errors'] is Map
            ? Map<String, dynamic>.from(json['errors'] as Map)
            : null,
      );
    }

    return ProblemDetailsException.fromStatus(status);
  }

  @override
  String toString() =>
      'ProblemDetailsException($status${code != null ? ', $code' : ''}): $detail';
}
