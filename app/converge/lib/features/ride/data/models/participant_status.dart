// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelho do enum `ParticipantStatus` do backend.
enum ParticipantStatus {
  requested('REQUESTED'),
  rejected('REJECTED'),
  confirmed('CONFIRMED'),
  cancelled('CANCELLED'),
  noShow('NO_SHOW'),
  completed('COMPLETED'),

  /// Valor que o app ainda não conhece (backend mais novo que o app).
  unknown('UNKNOWN');

  final String apiValue;

  const ParticipantStatus(this.apiValue);

  /// Quem tem a vaga garantida ou efetivamente viajou: é com essas pessoas que o chat fica liberado.
  bool get hasSeat => this == confirmed || this == completed;

  static ParticipantStatus fromJson(Object? value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () => unknown,
  );
}
