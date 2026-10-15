import 'dart:async';

import 'package:converge/core/services/auth/auth_exception.dart';
import 'package:converge/core/services/auth/auth_service.dart';
import 'package:converge/core/services/auth/auth_user.dart';

class FakeAuthService implements AuthService {
  final _changes = StreamController<AuthUser?>.broadcast();
  AuthUser? _current;
  int signOutCalls = 0;

  /// Erro lançado pelo próximo `signIn`/`signUp`.
  AuthException? nextError;

  FakeAuthService({AuthUser? initialUser}) : _current = initialUser;

  void _set(AuthUser? user) {
    _current = user;
    _changes.add(user);
  }

  @override
  Stream<AuthUser?> authStateChanges() async* {
    yield _current;
    yield* _changes.stream;
  }

  @override
  AuthUser? get currentUser => _current;

  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async =>
      _current == null ? null : 'token';

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

  @override
  Future<AuthUser> signUp({required String email, required String password}) =>
      signIn(email: email, password: password);

  @override
  Future<void> sendPasswordResetEmail(String email) async {}

  @override
  Future<void> sendEmailVerification() async {}

  @override
  Future<void> signOut() async {
    signOutCalls++;
    _set(null);
  }
}
