// Autor: Cristian Eduardo Fava - RA 25000636

import 'auth_user.dart';

/// Contrato do provedor de identidade. As features dependem desta interface,
/// nunca do SDK do Firebase diretamente.
abstract class AuthService {
  /// Emite o usuário atual ao assinar e a cada login/logout.
  Stream<AuthUser?> authStateChanges();

  AuthUser? get currentUser;

  /// ID token para o header `Authorization: Bearer`. Nulo se deslogado.
  ///
  /// Se [expectedUid] for informado, o token só é emitido para essa conta:
  /// lança `AuthException('session-changed')` quando o usuário atual for outro.
  Future<String?> getIdToken({bool forceRefresh = false, String? expectedUid});

  Future<AuthUser> signIn({required String email, required String password});

  Future<AuthUser> signUp({required String email, required String password});

  Future<void> sendPasswordResetEmail(String email);

  Future<void> sendEmailVerification();

  Future<void> signOut();
}
