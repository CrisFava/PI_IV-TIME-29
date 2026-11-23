// Autor: Cristian Eduardo Fava - RA 25000636

import 'dart:async';

import 'package:converge/core/services/auth/auth_exception.dart';
import 'package:converge/core/services/auth/auth_service.dart';
import 'package:converge/core/services/auth/auth_user.dart';

/// Estrutura e dados fake em memória para suporte a testes unitários e de widgets.
///
/// Faz o papel do Firebase Auth: login e cadastro sempre dão certo (com o UID `uid-1`), a não ser que
/// o teste programe [nextError], e toda troca de usuário é emitida em `authStateChanges`.
class FakeAuthService implements AuthService {
  /// Avisa os ouvintes a cada login e logout.
  final _changes = StreamController<AuthUser?>.broadcast();

  /// Usuário logado agora.
  AuthUser? _current;

  /// Quantas vezes `signOut` foi chamado.
  int signOutCalls = 0;

  /// Erro lançado pelo próximo `signIn`/`signUp`.
  AuthException? nextError;

  FakeAuthService({AuthUser? initialUser}) : _current = initialUser;

  /// Troca o usuário logado e avisa os ouvintes.
  void _set(AuthUser? user) {
    _current = user;
    _changes.add(user);
  }

  /// Emite o usuário atual e depois cada troca, como o stream do Firebase.
  @override
  Stream<AuthUser?> authStateChanges() async* {
    yield _current;
    yield* _changes.stream;
  }

  /// Usuário logado agora.
  @override
  AuthUser? get currentUser => _current;

  /// Devolve um token fixo (`token`) para quem está logado e confere a conta esperada como o serviço real.
  @override
  Future<String?> getIdToken({
    bool forceRefresh = false,
    String? expectedUid,
  }) async {
    final current = _current;
    if (expectedUid != null && current?.uid != expectedUid) {
      throw AuthException.sessionChanged();
    }
    return current == null ? null : 'token';
  }

  /// Loga como `uid-1` com o e-mail recebido, ou lança [nextError].
  @override
  Future<AuthUser> signIn({
    required String email,
    required String password,
  }) async {
    final error = nextError;
    if (error != null) throw error;
    final user = AuthUser(uid: 'uid-1', email: email, emailVerified: false);
    _set(user);
    return user;
  }

  /// Igual ao login: a conta "nasce" logada.
  @override
  Future<AuthUser> signUp({required String email, required String password}) =>
      signIn(email: email, password: password);

  /// Não faz nada.
  @override
  Future<void> sendPasswordResetEmail(String email) async {}

  /// Não faz nada.
  @override
  Future<void> sendEmailVerification() async {}

  /// Conta a chamada e desloga.
  @override
  Future<void> signOut() async {
    signOutCalls++;
    _set(null);
  }
}
