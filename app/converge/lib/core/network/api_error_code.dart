/// Espelho do enum `ErrorCode` do backend Spring Boot, mais os códigos
/// gerados no próprio app quando não há resposta válida do servidor.
enum ApiErrorCode {
  badRequest('BAD_REQUEST'),
  userNotFound('USER_NOT_FOUND'),
  emailAlreadyExists('EMAIL_ALREADY_EXISTS'),
  invalidCredentials('INVALID_CREDENTIALS'),
  notAllowed('NOT_ALLOWED'),

  /// Sem resposta do servidor (timeout, sem rede, servidor fora do ar).
  network('NETWORK'),

  /// Resposta fora do envelope `ApplicationResponse` ou com código desconhecido.
  unknown('UNKNOWN');

  final String value;

  const ApiErrorCode(this.value);

  static ApiErrorCode fromValue(Object? value) =>
      values.firstWhere((code) => code.value == value, orElse: () => unknown);
}
