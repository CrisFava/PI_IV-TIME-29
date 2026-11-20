// Autor: Cristian Eduardo Fava - RA 25000636

/// Falha do provedor de identidade com mensagem pronta para o usuário.
class AuthException implements Exception {
  final String code;
  final String message;

  const AuthException(this.code, this.message);

  /// A conta autenticada mudou durante uma operação vinculada a outra conta.
  factory AuthException.sessionChanged() => const AuthException(
    'session-changed',
    'Sua sessão mudou enquanto o perfil era salvo. Confira os dados e tente de novo.',
  );

  @override
  String toString() => 'AuthException($code): $message';
}
