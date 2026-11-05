// Autor: Cristian Eduardo Fava - RA 25000636

/// Usuário autenticado no provedor de identidade (Firebase Auth).
///
/// Representa só a identidade; o perfil da plataforma vem do backend.
class AuthUser {
  final String uid;
  final String? email;
  final bool emailVerified;

  const AuthUser({
    required this.uid,
    required this.email,
    required this.emailVerified,
  });
}
