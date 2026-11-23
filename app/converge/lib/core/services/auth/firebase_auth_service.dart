// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:firebase_auth/firebase_auth.dart' as fb;

import 'auth_service.dart';
import 'auth_exception.dart';
import 'auth_user.dart';

/// Implementação do serviço [AuthService] integrado ao Firebase Authentication.
///
/// Converte o `User` do SDK em [AuthUser] e todo `FirebaseAuthException` em [AuthException] com
/// mensagem em português, para nenhuma tela depender do SDK.
class FirebaseAuthService implements AuthService {
  /// Instância do SDK; nos testes não é usada, porque o serviço inteiro é trocado por um falso.
  final fb.FirebaseAuth _auth;

  FirebaseAuthService(this._auth);

  /// Repassa o stream do SDK convertendo cada usuário.
  @override
  Stream<AuthUser?> authStateChanges() => _auth.authStateChanges().map(_map);

  /// Usuário atual do SDK convertido.
  @override
  AuthUser? get currentUser => _map(_auth.currentUser);

  /// Pega o token do usuário atual. Com [expectedUid], confere a conta antes e depois de pedir o token,
  /// porque o login pode trocar enquanto o pedido está em andamento.
  @override
  Future<String?> getIdToken({
    bool forceRefresh = false,
    String? expectedUid,
  }) async {
    final user = _auth.currentUser;
    if (user == null) {
      if (expectedUid != null) throw AuthException.sessionChanged();
      return null;
    }
    if (expectedUid != null && user.uid != expectedUid) {
      throw AuthException.sessionChanged();
    }
    final token = await _guard(() => user.getIdToken(forceRefresh));
    // A conta pode ter mudado enquanto o token era obtido.
    if (expectedUid != null && _auth.currentUser?.uid != expectedUid) {
      throw AuthException.sessionChanged();
    }
    return token;
  }

  /// Login com o e-mail sem espaços nas pontas; a senha vai como foi digitada.
  @override
  Future<AuthUser> signIn({
    required String email,
    required String password,
  }) async {
    final credential = await _guard(
      () => _auth.signInWithEmailAndPassword(
        email: email.trim(),
        password: password,
      ),
    );
    return _map(credential.user)!;
  }

  /// Cria a conta com o e-mail sem espaços nas pontas.
  @override
  Future<AuthUser> signUp({
    required String email,
    required String password,
  }) async {
    final credential = await _guard(
      () => _auth.createUserWithEmailAndPassword(
        email: email.trim(),
        password: password,
      ),
    );
    return _map(credential.user)!;
  }

  /// Pede ao Firebase o e-mail de redefinição de senha.
  @override
  Future<void> sendPasswordResetEmail(String email) =>
      _guard(() => _auth.sendPasswordResetEmail(email: email.trim()));

  /// Pede ao Firebase o e-mail de verificação do usuário logado.
  @override
  Future<void> sendEmailVerification() async {
    final user = _auth.currentUser;
    if (user == null) return;
    await _guard(user.sendEmailVerification);
  }

  /// Sai da conta no Firebase; o `authStateChanges` emite nulo em seguida.
  @override
  Future<void> signOut() => _auth.signOut();

  /// Converte o usuário do SDK no modelo do app; nulo continua nulo.
  AuthUser? _map(fb.User? user) => user == null
      ? null
      : AuthUser(
          uid: user.uid,
          email: user.email,
          emailVerified: user.emailVerified,
        );

  /// Executa a chamada ao SDK trocando `FirebaseAuthException` por [AuthException].
  Future<T> _guard<T>(Future<T> Function() action) async {
    try {
      return await action();
    } on fb.FirebaseAuthException catch (e) {
      throw AuthException(e.code, _messageFor(e.code));
    }
  }

  /// Mensagem amigável para cada código de erro do Firebase Auth. Usuário inexistente e senha errada
  /// dão a mesma mensagem, para não revelar quais e-mails têm conta.
  static String _messageFor(String code) => switch (code) {
    'invalid-email' => 'E-mail inválido.',
    'user-disabled' => 'Esta conta foi desativada.',
    'user-not-found' ||
    'wrong-password' ||
    'invalid-credential' => 'E-mail ou senha incorretos.',
    'email-already-in-use' => 'Já existe uma conta com este e-mail.',
    'weak-password' => 'A senha deve ter pelo menos 6 caracteres.',
    'too-many-requests' =>
      'Muitas tentativas. Aguarde alguns minutos e tente novamente.',
    'network-request-failed' =>
      'Sem conexão. Verifique sua internet e tente novamente.',
    'requires-recent-login' => 'Por segurança, entre novamente na sua conta.',
    'operation-not-allowed' =>
      'Login com e-mail e senha não está habilitado no Firebase.',
    _ => 'Não foi possível autenticar. Tente novamente.',
  };
}
