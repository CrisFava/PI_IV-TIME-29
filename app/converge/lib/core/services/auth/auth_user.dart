// Autor: Cristian Eduardo Fava - RA 25000636

/// Usuário autenticado no provedor de identidade (Firebase Auth).
///
/// Representa só a identidade; o perfil da plataforma vem do backend.
class AuthUser {
  /// UID do Firebase, o mesmo `firebaseUid` gravado no usuário da API.
  final String uid;

  /// E-mail da conta; o Firebase pode não ter um em contas de outros provedores.
  final String? email;

  /// Se o usuário já confirmou o e-mail pelo link.
  final bool emailVerified;

  const AuthUser({
    required this.uid,
    required this.email,
    required this.emailVerified,
  });
}
