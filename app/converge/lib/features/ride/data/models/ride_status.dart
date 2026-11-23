// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelho do enum `RideStatus` do backend.
enum RideStatus {
  /// Criada a partir de um pedido de vaga, aguardando o aceite do motorista.
  awaitingConfirmation('AWAITING_CONFIRMATION', 'Aguardando confirmação'),
  /// Motorista já aceitou passageiro.
  confirmed('CONFIRMED', 'Confirmada'),

  /// O motorista iniciou o trajeto.
  inProgress('IN_PROGRESS', 'Em andamento'),

  /// Viagem encerrada.
  completed('COMPLETED', 'Concluída'),

  /// Cancelada pelo motorista, pela falta de passageiros ou por não ter sido iniciada a tempo.
  cancelled('CANCELLED', 'Cancelada'),

  /// Valor que o app ainda não conhece (backend mais novo que o app).
  unknown('UNKNOWN', 'Indefinida');

  /// Valor como o backend envia.
  final String apiValue;

  /// Texto em português para a tela.
  final String label;

  const RideStatus(this.apiValue, this.label);

  /// Lê o valor da API; desconhecido ou ausente vira [unknown].
  static RideStatus fromJson(Object? value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () => unknown,
  );
}
