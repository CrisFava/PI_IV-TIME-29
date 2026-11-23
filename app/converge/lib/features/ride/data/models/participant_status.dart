// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelho do enum `ParticipantStatus` do backend.
enum ParticipantStatus {
  /// Pediu a vaga e espera a resposta do motorista (RF19).
  requested('REQUESTED'),

  /// O motorista recusou o pedido.
  rejected('REJECTED'),

  /// Vaga confirmada pelo motorista.
  confirmed('CONFIRMED'),

  /// Saiu da viagem ou foi removido antes dela.
  cancelled('CANCELLED'),

  /// O motorista registrou a ausência (RF23).
  noShow('NO_SHOW'),

  /// Concluiu a viagem (desceu ou a viagem foi encerrada com ele a bordo).
  completed('COMPLETED'),

  /// O pedido expirou sem resposta do motorista.
  expired('EXPIRED'),

  /// Valor que o app ainda não conhece (backend mais novo que o app).
  unknown('UNKNOWN');

  /// Valor como o backend envia.
  final String apiValue;

  const ParticipantStatus(this.apiValue);

  /// Quem tem a vaga garantida ou efetivamente viajou: é com essas pessoas que o chat fica liberado.
  bool get hasSeat => this == confirmed || this == completed;

  /// Lê o valor da API; desconhecido ou ausente vira [unknown].
  static ParticipantStatus fromJson(Object? value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () => unknown,
  );
}
