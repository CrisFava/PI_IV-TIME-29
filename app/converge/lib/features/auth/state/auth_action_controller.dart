// Autor: Cristian Eduardo Fava - RA 25000636

// Coautor: Allan Giovanni Matias Paes - RA 25008211 (métodos: becomeDriver())

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/services/auth/auth_exception.dart';
import '../../vehicle/data/validation/invalid_vehicle_exception.dart';
import 'auth_controller.dart';
import 'inputs/profile_input.dart';
import 'inputs/register_input.dart';

/// Estado de envio dos formulários de autenticação. É `autoDispose`: cada tela começa sem erro.
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
  /// Começa parado, sem erro.
  @override
  AsyncValue<void> build() => const AsyncData(null);

  /// Controller da sessão, que faz o trabalho de fato.
  AuthController get _session => ref.read(authControllerProvider.notifier);

  /// Login com e-mail e senha (RF04).
  Future<bool> signIn({required String email, required String password}) =>
      _run(() => _session.signIn(email: email, password: password));

  /// Cria a conta no Firebase e o cadastro na API (RF01, RF02).
  Future<bool> register(RegisterInput input) =>
      _run(() => _session.register(input));

  /// Completa o cadastro de quem tem conta no Firebase mas ainda não tem perfil na API.
  Future<bool> completeProfile(ProfileInput profile) =>
      _run(() => _session.completeProfile(profile));

  /// Torna o usuário logado motorista.
  Future<bool> becomeDriver() => _run(_session.becomeDriver);

  /// Envia o e-mail de redefinição de senha.
  Future<bool> sendPasswordResetEmail(String email) =>
      _run(() => _session.sendPasswordResetEmail(email));

  /// Sai da conta (RF05).
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
///
/// [AuthException], [ApiException] e `InvalidVehicleException` já trazem a mensagem pronta; qualquer
/// outro erro vira uma mensagem genérica, sem expor detalhes técnicos na tela.
String authErrorMessage(Object error) => switch (error) {
  AuthException(:final message) => message,
  ApiException(:final message) => message,
  InvalidVehicleException(:final message) => message,
  _ => 'Algo deu errado. Tente novamente.',
};
