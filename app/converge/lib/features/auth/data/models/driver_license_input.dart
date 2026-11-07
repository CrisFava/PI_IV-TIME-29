// Autor: Allan Giovanni Matias Paes - RA 25008211

enum DriverLicenseCategory {
  a, b, ab, c, d, e;

  String toJson() => name.toUpperCase();

  static DriverLicenseCategory? fromJson(String? value) =>
      values.asNameMap()[value?.toLowerCase()];

  String get label => toJson();
}

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
