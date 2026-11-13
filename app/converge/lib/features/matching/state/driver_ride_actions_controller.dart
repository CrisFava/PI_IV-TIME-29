// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/state/auth_action_controller.dart';
import '../../ride/data/repository/ride_repository.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import '../../ride/state/ride_providers.dart';

final driverRideActionsProvider =
    NotifierProvider.autoDispose<DriverRideActionsController, Set<String>>(
      DriverRideActionsController.new,
    );

/// Iniciar e concluir as viagens do motorista. O estado são as viagens com uma
/// ação em andamento (para desabilitar os botões).
class DriverRideActionsController extends Notifier<Set<String>> {
  RideRepository get _rides => ref.read(rideRepositoryProvider);

  @override
  Set<String> build() => const {};

  /// "Iniciar trajeto": a viagem começa e o motorista entra em expediente.
  /// Devolve a mensagem de erro, ou `null` se deu certo.
  Future<String?> start(String rideId) =>
      _run(rideId, () => _rides.startRide(rideId));

  /// Encerra o expediente concluindo a viagem. Devolve a mensagem de erro, ou `null`.
  Future<String?> complete(String rideId) =>
      _run(rideId, () => _rides.completeRide(rideId));

  Future<String?> _run(String rideId, Future<Object?> Function() action) async {
    if (state.contains(rideId)) return null;
    state = {...state, rideId};

    try {
      await action();
      ref.invalidate(myRidesProvider);
      return null;
    } catch (e) {
      return authErrorMessage(e);
    } finally {
      if (ref.mounted) state = {...state}..remove(rideId);
    }
  }
}
