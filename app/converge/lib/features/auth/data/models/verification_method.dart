/// Espelha `VerificationMethod` do backend.
enum VerificationMethod {
  institutionalEmail,
  document;

  String toJson() => switch (this) {
    institutionalEmail => 'INSTITUTIONAL_EMAIL',
    document => 'DOCUMENT',
  };

  static VerificationMethod fromJson(String? value) => values.firstWhere(
    (method) => method.toJson() == value,
    orElse: () => institutionalEmail,
  );
}
