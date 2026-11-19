// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/state/auth_action_controller.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import '../../ride/state/ride_providers.dart';

final passengerRideActionsProvider =
    NotifierProvider.autoDispose<PassengerRideActionsController, Set<String>>(
      PassengerRideActionsController.new,
    );

/// Ações do passageiro numa viagem em que já foi aceito. O estado são as viagens
/// com uma ação em andamento (para desabilitar os botões).
class PassengerRideActionsController extends Notifier<Set<String>> {
  @override
  Set<String> build() => const {};

  /// "Sair da viagem" (antes de ela começar). Devolve a mensagem de erro, ou `null`.
  Future<String?> leave(String rideId) async {
    if (state.contains(rideId)) return null;
    state = {...state, rideId};

    try {
      await ref
          .read(rideRepositoryProvider)
          .leaveRide(rideId, reason: 'O passageiro saiu da viagem.');
      ref.invalidate(myRidesProvider);
      return null;
    } catch (e) {
      return authErrorMessage(e);
    } finally {
      if (ref.mounted) state = {...state}..remove(rideId);
    }
  }
}
