import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/problem_details.dart';
import '../../../core/services/auth/auth_exception.dart';
import 'auth_controller.dart';
import 'inputs/profile_input.dart';
import 'inputs/register_input.dart';

final authActionProvider =
    NotifierProvider.autoDispose<AuthActionController, AsyncValue<void>>(
      AuthActionController.new,
    );

/// Estado de envio dos formulários de autenticação (loading / erro).
///
/// As telas observam este provider para desabilitar botões e mostrar
/// [authErrorMessage]; a navegação acontece sozinha pelos guards de rota
/// quando a sessão muda.
class AuthActionController extends Notifier<AsyncValue<void>> {
  @override
  AsyncValue<void> build() => const AsyncData(null);

  AuthController get _session => ref.read(authControllerProvider.notifier);

  Future<bool> signIn({required String email, required String password}) =>
      _run(() => _session.signIn(email: email, password: password));

  Future<bool> register(RegisterInput input) =>
      _run(() => _session.register(input));

  Future<bool> completeProfile(ProfileInput profile) =>
      _run(() => _session.completeProfile(profile));

  Future<bool> sendPasswordResetEmail(String email) =>
      _run(() => _session.sendPasswordResetEmail(email));

  Future<bool> signOut() => _run(_session.signOut);

  /// Ignora novos envios enquanto um estiver em andamento.
  /// Retorna `true` em caso de sucesso.
  Future<bool> _run(Future<void> Function() action) async {
    if (state.isLoading) return false;
    state = const AsyncLoading();
    final result = await AsyncValue.guard(action);
    if (ref.mounted) state = result;
    return !result.hasError;
  }
}

/// Mensagem amigável para qualquer erro vindo das ações de autenticação.
String authErrorMessage(Object error) => switch (error) {
  AuthException(:final message) => message,
  ProblemDetailsException(:final detail) => detail,
  _ => 'Algo deu errado. Tente novamente.',
};
