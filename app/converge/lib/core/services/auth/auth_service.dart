import 'auth_user.dart';

/// Contrato do provedor de identidade. As features dependem desta interface,
/// nunca do SDK do Firebase diretamente.
abstract class AuthService {
  /// Emite o usuário atual ao assinar e a cada login/logout.
  Stream<AuthUser?> authStateChanges();

  AuthUser? get currentUser;

  /// ID token para o header `Authorization: Bearer`. Nulo se deslogado.
  Future<String?> getIdToken({bool forceRefresh = false});

  Future<AuthUser> signIn({required String email, required String password});

  Future<AuthUser> signUp({required String email, required String password});

  Future<void> sendPasswordResetEmail(String email);

  Future<void> sendEmailVerification();

  Future<void> signOut();
}
