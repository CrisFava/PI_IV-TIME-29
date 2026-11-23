// Autor: Cristian Eduardo Fava - RA 25000636

import 'profile_input.dart';

/// Dados do formulário de criação de conta.
class RegisterInput {
  /// E-mail da conta nova no Firebase.
  final String email;

  /// Senha da conta nova (mínimo de 6 caracteres, regra do Firebase).
  final String password;

  /// Dados de perfil enviados à API depois de criar a conta.
  final ProfileInput profile;

  const RegisterInput({
    required this.email,
    required this.password,
    required this.profile,
  });
}
