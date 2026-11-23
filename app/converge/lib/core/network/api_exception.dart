// Autor: Cristian Eduardo Fava - RA 25000636

import 'api_error_code.dart';

/// Erro de API no formato normalizado pelo backend:
/// `ApplicationResponse { success: false, error: { code, message, fields } }`,
/// acrescido do status HTTP da resposta.
class ApiException implements Exception {
  /// Código do erro, para a tela decidir o que fazer.
  final ApiErrorCode code;

  /// Mensagem pronta para exibir ao usuário (o backend já manda em português).
  final String message;

  /// Campos do formulário relacionados ao erro, para destacar na tela.
  final Set<String> fields;

  /// Status HTTP da resposta; [networkStatus] quando não houve resposta.
  final int status;

  const ApiException({
    required this.code,
    required this.message,
    required this.status,
    this.fields = const {},
  });

  /// Status usado quando não houve resposta do servidor.
  static const int networkStatus = 0;

  /// Sem resposta do servidor.
  bool get isNetworkError => code == ApiErrorCode.network;

  /// HTTP 404.
  bool get isNotFound => status == 404;

  /// HTTP 409.
  bool get isConflict => status == 409;

  /// Cobrança em aberto bloqueando a ação (RF74).
  bool get isPaymentPending => code == ApiErrorCode.paymentPending;

  /// HTTP 401: sessão expirada ou token inválido.
  bool get isUnauthorized => status == 401;

  /// Erro de rede, com a mensagem padrão de falta de conexão.
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

  /// Mensagem genérica por faixa de status, usada quando o backend não mandou uma.
  static String _defaultMessage(int status) => switch (status) {
    401 => 'Sua sessão expirou. Entre novamente.',
    403 => 'Você não tem permissão para esta ação.',
    404 => 'Recurso não encontrado.',
    409 => 'O recurso já existe.',
    >= 500 => 'O servidor encontrou um erro. Tente novamente mais tarde.',
    _ => 'Ocorreu um erro ao processar sua solicitação.',
  };

  /// Texto para log, com status, código e mensagem.
  @override
  String toString() => 'ApiException($status, ${code.value}): $message';
}
