// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/state/auth_action_controller.dart';
import '../../ride/data/repository/ride_repository.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import '../../ride/state/ride_providers.dart';
import '../data/models/driver_ride_request.dart';

final rideRequestActionsProvider =
    NotifierProvider.autoDispose<RideRequestActionsController, Set<String>>(
      RideRequestActionsController.new,
    );

/// Respostas do motorista aos pedidos de vaga.
/// O estado é o conjunto de pedidos com uma resposta em andamento (para desabilitar os botões).
class RideRequestActionsController extends Notifier<Set<String>> {
  RideRepository get _rides => ref.read(rideRepositoryProvider);

  @override
  Set<String> build() => const {};

  bool isBusy(DriverRideRequest request) => state.contains(request.key);

  /// Aceita o passageiro, o que libera o chat. Devolve a mensagem de erro, ou `null` se deu certo.
  Future<String?> accept(DriverRideRequest request) => _run(
    request,
    () => _rides.acceptPassenger(request.rideId, request.passengerId),
  );

  /// Recusa o passageiro. Devolve a mensagem de erro, ou `null` se deu certo.
  Future<String?> reject(DriverRideRequest request) => _run(
    request,
    () => _rides.rejectPassenger(request.rideId, request.passengerId),
  );

  Future<String?> _run(
    DriverRideRequest request,
    Future<Object?> Function() action,
  ) async {
    if (isBusy(request)) return null;
    state = {...state, request.key};

    try {
      await action();
      ref.invalidate(myRidesProvider);
      return null;
    } catch (e) {
      return authErrorMessage(e);
    } finally {
      if (ref.mounted) state = {...state}..remove(request.key);
    }
  }
}
