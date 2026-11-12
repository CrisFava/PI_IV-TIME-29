// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelho do enum `ParticipantPaymentStatus` do backend.
enum PaymentStatus {
  pending('PENDING'),
  awaitingDriverConfirmation('AWAITING_DRIVER_CONFIRMATION'),
  paid('PAID'),
  notApplicable('NOT_APPLICABLE'),

  /// Valor que o app ainda não conhece (backend mais novo que o app).
  unknown('UNKNOWN');

  final String apiValue;

  const PaymentStatus(this.apiValue);

  static PaymentStatus fromJson(Object? value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () => unknown,
  );
}
