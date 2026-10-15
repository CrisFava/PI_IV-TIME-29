import 'profile_input.dart';

/// Dados do formulário de criação de conta.
class RegisterInput {
  final String email;
  final String password;
  final ProfileInput profile;

  const RegisterInput({
    required this.email,
    required this.password,
    required this.profile,
  });
}
