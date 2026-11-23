// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelha `VerificationMethod` do backend.
enum VerificationMethod {
  /// Confirmação automática pelo link enviado ao e-mail institucional (padrão no MVP).
  institutionalEmail,

  /// Envio de documento revisado por um moderador.
  document;

  /// Valor como o backend espera.
  String toJson() => switch (this) {
    institutionalEmail => 'INSTITUTIONAL_EMAIL',
    document => 'DOCUMENT',
  };

  /// Lê o valor da API; desconhecido ou nulo vira [institutionalEmail].
  static VerificationMethod fromJson(String? value) => values.firstWhere(
    (method) => method.toJson() == value,
    orElse: () => institutionalEmail,
  );
}
