// Autor: Cristian Eduardo Fava - RA 25000636

import 'auth_user.dart';

/// Contrato do provedor de identidade. As features dependem desta interface,
/// nunca do SDK do Firebase diretamente.
abstract class AuthService {
  /// Emite o usuário atual ao assinar e a cada login/logout.
  Stream<AuthUser?> authStateChanges();

  /// Usuário logado agora, ou nulo se não houver sessão.
  AuthUser? get currentUser;

  /// ID token para o header `Authorization: Bearer`. Nulo se deslogado.
  ///
  /// Se [expectedUid] for informado, o token só é emitido para essa conta:
  /// lança `AuthException('session-changed')` quando o usuário atual for outro.
  Future<String?> getIdToken({bool forceRefresh = false, String? expectedUid});

  /// Entra com e-mail e senha. Lança [AuthException] com a mensagem pronta se falhar.
  Future<AuthUser> signIn({required String email, required String password});

  /// Cria a conta no provedor de identidade e já deixa o usuário logado.
  /// O cadastro na API é feito depois, pelo repositório de autenticação.
  Future<AuthUser> signUp({required String email, required String password});

  /// Envia o e-mail de redefinição de senha.
  Future<void> sendPasswordResetEmail(String email);

  /// Envia o e-mail de verificação para o usuário logado; não faz nada sem sessão.
  Future<void> sendEmailVerification();

  /// Encerra a sessão (RF05).
  Future<void> signOut();
}
