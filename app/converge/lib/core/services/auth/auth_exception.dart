// Autor: Cristian Eduardo Fava - RA 25000636

/// Falha do provedor de identidade com mensagem pronta para o usuário.
///
/// É o par da [ApiException] para erros do Firebase Auth; a função `authErrorMessage` trata as duas.
class AuthException implements Exception {
  /// Código do Firebase (ex.: `wrong-password`) ou `session-changed`, criado pelo app.
  final String code;

  /// Mensagem em português para exibir na tela.
  final String message;

  const AuthException(this.code, this.message);

  /// A conta autenticada mudou durante uma operação vinculada a outra conta.
  factory AuthException.sessionChanged() => const AuthException(
    'session-changed',
    'Sua sessão mudou enquanto o perfil era salvo. Confira os dados e tente de novo.',
  );

  /// Texto para log, com o código e a mensagem.
  @override
  String toString() => 'AuthException($code): $message';
}
