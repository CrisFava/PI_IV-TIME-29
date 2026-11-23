// Autor: Cristian Eduardo Fava - RA 25000636

/// Espelha `VerificationStatus` do backend.
///
/// Serve tanto para o vínculo acadêmico quanto para a CNH; só a CNH usa [expired].
enum VerificationStatus {
  /// Aprovado.
  verified,

  /// Enviado e aguardando análise.
  underReview,

  /// Recusado; o motivo vem em `rejectionReason`.
  rejected,

  /// Ainda não enviado ou não analisado.
  pending,

  /// CNH vencida.
  expired;

  /// Valor como o backend espera.
  String toJson() => switch (this) {
    verified => 'VERIFIED',
    underReview => 'UNDER_REVIEW',
    rejected => 'REJECTED',
    pending => 'PENDING',
    expired => 'EXPIRED',
  };

  /// Lê o valor da API; desconhecido ou nulo vira [pending].
  static VerificationStatus fromJson(String? value) => values.firstWhere(
    (status) => status.toJson() == value,
    orElse: () => pending,
  );
}
