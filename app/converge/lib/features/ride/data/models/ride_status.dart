// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelho do enum `RideStatus` do backend.
enum RideStatus {
  awaitingConfirmation('AWAITING_CONFIRMATION', 'Aguardando confirmação'),
  confirmed('CONFIRMED', 'Confirmada'),
  inProgress('IN_PROGRESS', 'Em andamento'),
  completed('COMPLETED', 'Concluída'),
  cancelled('CANCELLED', 'Cancelada'),

  /// Valor que o app ainda não conhece (backend mais novo que o app).
  unknown('UNKNOWN', 'Indefinida');

  final String apiValue;
  final String label;

  const RideStatus(this.apiValue, this.label);

  static RideStatus fromJson(Object? value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () => unknown,
  );
}
