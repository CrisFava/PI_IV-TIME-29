// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelho do enum `ErrorCode` do backend Spring Boot, mais os códigos
/// gerados no próprio app quando não há resposta válida do servidor.
enum ApiErrorCode {
  /// Dados inválidos ou regra de negócio violada; `fields` diz quais campos.
  badRequest('BAD_REQUEST'),

  /// O usuário procurado não existe (inclusive quem ainda não completou o cadastro na API).
  userNotFound('USER_NOT_FOUND'),

  /// E-mail já usado por outra conta.
  emailAlreadyExists('EMAIL_ALREADY_EXISTS'),

  /// Token do Firebase ausente, inválido ou expirado.
  invalidCredentials('INVALID_CREDENTIALS'),

  /// O usuário está logado, mas não pode fazer essa ação (ex.: não é o motorista da viagem).
  notAllowed('NOT_ALLOWED'),

  /// Cobrança de uma carona em aberto: não dá para pedir vaga nem cadastrar trajeto.
  paymentPending('PAYMENT_PENDING'),

  /// Sem resposta do servidor (timeout, sem rede, servidor fora do ar).
  network('NETWORK'),

  /// Resposta fora do envelope `ApplicationResponse` ou com código desconhecido.
  unknown('UNKNOWN');

  /// Código como o backend envia no campo `error.code`.
  final String value;

  const ApiErrorCode(this.value);

  /// Converte o código recebido no enum; código desconhecido ou ausente vira [unknown].
  static ApiErrorCode fromValue(Object? value) =>
      values.firstWhere((code) => code.value == value, orElse: () => unknown);
}
