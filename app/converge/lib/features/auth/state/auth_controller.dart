import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/services/auth/auth_providers.dart';
import '../../../core/services/auth/auth_service.dart';
import '../data/models/user_model.dart';
import '../data/repository/auth_repository.dart';
import '../data/repository/auth_repository_impl.dart';
import 'inputs/profile_input.dart';
import 'inputs/register_input.dart';
import 'session/auth_session.dart';
import '../../../core/services/auth/auth_exception.dart';
import '../../../core/services/auth/auth_user.dart';

final authControllerProvider =
    AsyncNotifierProvider<AuthController, AuthSession>(AuthController.new);

/// Dono da sessão: combina a identidade do Firebase com o perfil do backend.
class AuthController extends AsyncNotifier<AuthSession> {
  AuthService get _auth => ref.read(authServiceProvider);
  AuthRepository get _repository => ref.read(authRepositoryProvider);

  @override
  Future<AuthSession> build() async {
    final auth = ref.watch(authServiceProvider);

    // Reage a logout feito fora do controller (token revogado, conta
    // removida). Logins são tratados pelas próprias ações.
    final subscription = auth.authStateChanges().listen((user) {
      if (user == null && state.value is! Unauthenticated) {
        state = const AsyncData(Unauthenticated());
      }
    });
    ref.onDispose(subscription.cancel);

    // Espera o Firebase restaurar a sessão persistida antes de decidir.
    final user = await auth.authStateChanges().first;
    if (user == null) return const Unauthenticated();
    return _loadSession(user);
  }

  /// Recarrega a sessão do zero (após falha na abertura do app).
  void retry() => ref.invalidateSelf();

  Future<void> signIn({required String email, required String password}) async {
    final user = await _auth.signIn(email: email, password: password);
    try {
      state = AsyncData(await _loadSession(user));
    } catch (_) {
      // Sem perfil carregado, não mantém o usuário logado pela metade.
      await _auth.signOut();
      rethrow;
    }
  }

  /// Cria a conta no Firebase e o cadastro no backend.
  Future<void> register(RegisterInput input) async {
    final user = await _auth.signUp(
      email: input.email,
      password: input.password,
    );
    await _createProfile(user, input.profile);
  }

  /// Conclui o cadastro de quem está em [ProfileIncomplete].
  Future<void> completeProfile(ProfileInput profile) async {
    final user = _auth.currentUser;
    if (user == null) {
      state = const AsyncData(Unauthenticated());
      throw const AuthException(
        'no-current-user',
        'Sua sessão expirou. Entre novamente.',
      );
    }
    await _createProfile(user, profile);
  }

  /// Recarrega o perfil do backend, mantendo a sessão atual em caso de erro.
  Future<void> refreshProfile() async {
    final user = _auth.currentUser;
    if (user == null) return;
    state = AsyncData(await _loadSession(user));
  }

  Future<void> sendPasswordResetEmail(String email) =>
      _auth.sendPasswordResetEmail(email);

  Future<void> signOut() async {
    await _auth.signOut();
    state = const AsyncData(Unauthenticated());
  }

  Future<AuthSession> _loadSession(AuthUser user) async {
    try {
      return Authenticated(await _repository.fetchCurrentUser());
    } on ApiException catch (e) {
      if (e.isNotFound) return ProfileIncomplete(user);
      rethrow;
    }
  }

  Future<void> _createProfile(AuthUser user, ProfileInput profile) async {
    final email = user.email;
    if (email == null) {
      throw const AuthException(
        'missing-email',
        'Sua conta não possui e-mail. Entre com e-mail e senha.',
      );
    }

    try {
      final created = await _repository.createUser(
        profile.toRequest(firebaseUid: user.uid, email: email),
      );
      state = AsyncData(Authenticated(created));
    } on ApiException catch (e) {
      // 409 pode significar que uma tentativa anterior (ex.: timeout) já
      // criou o cadastro. Se o perfil existir, a sessão está completa.
      if (e.isConflict) {
        final existing = await _tryFetchCurrentUser();
        if (existing != null) {
          state = AsyncData(Authenticated(existing));
          return;
        }
      }
      state = AsyncData(ProfileIncomplete(user));
      rethrow;
    }
  }

  Future<UserModel?> _tryFetchCurrentUser() async {
    try {
      return await _repository.fetchCurrentUser();
    } on ApiException {
      return null;
    }
  }
}
