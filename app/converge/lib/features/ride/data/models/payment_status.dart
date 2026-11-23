// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelho do enum `ParticipantPaymentStatus` do backend.
enum PaymentStatus {
  /// Ainda não pago. Só é uma cobrança em aberto depois de emitida (com valor).
  pending('PENDING'),

  /// O passageiro informou o pagamento ("Já paguei", RF41).
  paid('PAID'),

  /// Sem nada a cobrar: ausente, cancelado ou viagem sem custo calculado.
  notApplicable('NOT_APPLICABLE'),

  /// Valor que o app ainda não conhece (backend mais novo que o app).
  unknown('UNKNOWN');

  /// Valor como o backend envia.
  final String apiValue;

  const PaymentStatus(this.apiValue);

  /// Lê o valor da API; desconhecido ou ausente vira [unknown].
  static PaymentStatus fromJson(Object? value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () => unknown,
  );
}
