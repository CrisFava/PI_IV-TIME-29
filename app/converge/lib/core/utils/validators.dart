// Autor: Cristian Eduardo Fava - RA 25000636

/// Validadores no formato de `FormField.validator`: devolvem a mensagem de
/// erro ou `null` se o valor for válido.
abstract class Validators {
  /// Formato mínimo de e-mail: algo, arroba, domínio com ponto e sem espaços.
  static final RegExp _email = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$');

  /// Formato aceito pelo backend: `(19) 91234-5678` ou `(19) 3123-4567`.
  static final RegExp _phone = RegExp(r'^\(\d{2}\) \d{4,5}-\d{4}$');

  /// Mínimo exigido pelo Firebase Auth.
  static const int minPasswordLength = 6;

  /// Mínimo exigido pelo backend (`User.name`).
  static const int minNameLength = 3;

  /// Campo obrigatório: recusa nulo, vazio ou só espaços. [field] entra na mensagem.
  static String? required(String? value, {String field = 'Campo'}) =>
      (value == null || value.trim().isEmpty) ? '$field é obrigatório' : null;

  /// E-mail obrigatório e no formato `nome@dominio.algo`.
  static String? email(String? value) {
    if (value == null || value.trim().isEmpty) return 'E-mail é obrigatório';
    if (!_email.hasMatch(value.trim())) return 'Formato de e-mail inválido';
    return null;
  }

  /// Senha obrigatória com pelo menos [minPasswordLength] caracteres (espaços contam).
  static String? password(String? value) {
    if (value == null || value.isEmpty) return 'Senha é obrigatória';
    if (value.length < minPasswordLength) {
      return 'A senha deve ter pelo menos $minPasswordLength caracteres';
    }
    return null;
  }

  /// Confirmação de senha: obrigatória e igual a [password].
  static String? confirmPassword(String? value, String password) {
    if (value == null || value.isEmpty) return 'Confirme sua senha';
    if (value != password) return 'As senhas não conferem';
    return null;
  }

  /// Nome obrigatório com pelo menos [minNameLength] caracteres, sem contar os espaços das pontas.
  static String? name(String? value) {
    if (value == null || value.trim().isEmpty) return 'Nome é obrigatório';
    if (value.trim().length < minNameLength) {
      return 'Nome deve ter no mínimo $minNameLength caracteres';
    }
    return null;
  }

  /// Telefone obrigatório no formato com DDD, fixo ou celular.
  static String? phone(String? value) {
    if (value == null || value.trim().isEmpty) return 'Telefone é obrigatório';
    if (!_phone.hasMatch(value.trim())) {
      return 'Use o formato (00) 90000-0000';
    }
    return null;
  }
}
