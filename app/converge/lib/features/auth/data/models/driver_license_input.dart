// Autor: Allan Giovanni Matias Paes - RA 25008211

enum DriverLicenseCategory {
  a,
  b,
  ab,
  c,
  d,
  e;

  String toJson() => switch (this) {
    a => 'A',
    b => 'B',
    ab => 'AB',
    c => 'C',
    d => 'D',
    e => 'E',
  };

  static DriverLicenseCategory? fromJson(String? value) =>
      switch (value?.toUpperCase()) {
        'A' => a,
        'B' => b,
        'AB' => ab,
        'C' => c,
        'D' => d,
        'E' => e,
        _ => null,
      };

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
