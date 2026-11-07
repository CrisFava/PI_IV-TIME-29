// Autor: Allan Giovanni Matias Paes - RA 25008211

enum PixKeyType {
  cpf('CPF', 'CPF'),
  email('EMAIL', 'E-mail'),
  phone('PHONE', 'Telefone'),
  random('RANDOM', 'Chave Aleatória');

  const PixKeyType(this.code, this.label);
  final String code;
  final String label;

  String toJson() => code;

  static PixKeyType? fromJson(String? value) {
    final upper = value?.toUpperCase();
    for (final t in values) {
      if (t.code == upper) return t;
    }
    return null;
  }
}