// Autor: Allan Giovanni Matias Paes - RA 25008211

enum PixKeyType {
  cpf,
  email,
  phone,
  random;

  String toJson() => switch (this) {
    cpf => 'CPF',
    email => 'EMAIL',
    phone => 'PHONE',
    random => 'RANDOM',
  };

  static PixKeyType? fromJson(String? value) => switch (value?.toUpperCase()) {
    'CPF' => cpf,
    'EMAIL' => email,
    'PHONE' => phone,
    'RANDOM' => random,
    _ => null,
  };

  String get label => switch (this) {
    cpf => 'CPF',
    email => 'E-mail',
    phone => 'Telefone',
    random => 'Chave Aleatória',
  };
}
