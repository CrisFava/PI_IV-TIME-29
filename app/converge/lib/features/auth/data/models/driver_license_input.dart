// Autor: Allan Giovanni Matias Paes - RA 25008211

/// Categorias de Carteira Nacional de Habilitação (CNH) aceitas pela plataforma.
enum DriverLicenseCategory {
  a,
  b,
  ab,
  c,
  d,
  e;

  String toJson() => name.toUpperCase();

  static DriverLicenseCategory? fromJson(String? value) =>
      values.asNameMap()[value?.toLowerCase()];

  String get label => toJson();
}

/// Dados de entrada para submissão e validação de CNH do motorista.
class DriverLicenseInput {
  final String number;
  final DriverLicenseCategory category;
  final DateTime expiresAt;

  const DriverLicenseInput({
    required this.number,
    required this.category,
    required this.expiresAt,
  });
}
