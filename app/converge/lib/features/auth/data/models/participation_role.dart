// Autor: Cristian Eduardo Fava - RA 25000636

/// Papéis de participação do usuário no ecossistema do Converge (passageiro e/ou motorista).
enum ParticipationRole {
  /// Procura vagas em caronas.
  passenger,

  /// Oferece carona com veículo próprio.
  driver;

  /// Valor como o backend espera (`PASSENGER` ou `DRIVER`).
  String toJson() => switch (this) {
    passenger => 'PASSENGER',
    driver => 'DRIVER',
  };

  /// Lê o valor da API sem diferenciar maiúsculas; desconhecido ou nulo vira `null`.
  static ParticipationRole? fromJson(String? value) {
    if (value == null) return null;
    final normalized = value.trim().toUpperCase();
    for (final role in values) {
      if (role.toJson() == normalized) return role;
    }
    return null;
  }
}
