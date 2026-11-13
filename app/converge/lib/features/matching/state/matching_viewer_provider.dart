// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/state/auth_controller.dart';
import '../../auth/state/session/auth_session.dart';

/// Id do usuário logado no backend, ou `null` fora de uma sessão completa.
final matchingViewerIdProvider = Provider.autoDispose<String?>((ref) {
  final session = ref.watch(authControllerProvider).value;
  return session is Authenticated ? session.user.id : null;
});

/// De quanto em quanto tempo o passageiro consulta se o motorista respondeu.
final matchingPollIntervalProvider = Provider<Duration>(
  (ref) => const Duration(seconds: 5),
);
