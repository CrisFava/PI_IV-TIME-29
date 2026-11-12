// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../auth/state/auth_action_controller.dart';
import '../../ride/data/models/participant_status.dart';
import '../../ride/data/models/ride_model.dart';
import '../../ride/data/repository/ride_repository.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import '../../route/data/models/create_route_request.dart';
import '../../route/data/models/route_response_model.dart';
import '../../route/data/repository/route_repository.dart';
import '../../route/data/repository/route_repository_impl.dart';
import '../data/models/match_model.dart';
import '../data/repository/matching_repository.dart';
import '../data/repository/matching_repository_impl.dart';
import 'matching_viewer_provider.dart';
import 'passenger_match_phase.dart';
import 'passenger_match_state.dart';

final passengerMatchProvider =
    NotifierProvider.autoDispose<PassengerMatchController, PassengerMatchState>(
      PassengerMatchController.new,
    );

/// Fluxo do passageiro antes do aceite: busca os motoristas compatíveis,
/// pede a vaga e acompanha a viagem até o motorista aceitar ou recusar.
class PassengerMatchController extends Notifier<PassengerMatchState> {
  Timer? _polling;

  MatchingRepository get _matching => ref.read(matchingRepositoryProvider);
  RideRepository get _rides => ref.read(rideRepositoryProvider);
  RouteRepository get _routes => ref.read(routeRepositoryProvider);

  @override
  PassengerMatchState build() {
    ref.onDispose(_stopPolling);
    return const PassengerMatchState();
  }

  /// Id de um trajeto de procura com estes dados: reaproveita o que já existe
  /// ou cria um novo. Devolve `null` (com o motivo em `notice`) se não conseguir.
  Future<String?> prepareSearchRoute(CreateRouteRequest request) async {
    try {
      final existing = _findSame(await _routes.listMyRoutes(), request);
      if (existing != null) return existing.id;

      return (await _routes.createRoute(request)).id;
    } on ApiException catch (e) {
      // Outro pedido salvou o mesmo trajeto no meio do caminho: usa o que ficou no banco.
      if (e.isConflict) {
        final saved = _findSame(await _routes.listMyRoutes(), request);
        if (saved != null) return saved.id;
      }
      state = state.copyWith(notice: e.message);
      return null;
    } catch (e) {
      state = state.copyWith(notice: authErrorMessage(e));
      return null;
    }
  }

  RouteResponseModel? _findSame(
    List<RouteResponseModel> routes,
    CreateRouteRequest request,
  ) {
    for (final route in routes) {
      if (route.type == request.type &&
          route.status == 'ACTIVE' &&
          route.origin.address == request.origin.address &&
          route.destination.address == request.destination.address &&
          route.departureTime == request.departureTime) {
        return route;
      }
    }
    return null;
  }

  /// Busca os motoristas compatíveis com o trajeto de procura.
  Future<void> findDrivers(String searchRouteId) async {
    _stopPolling();
    state = PassengerMatchState(
      phase: PassengerMatchPhase.loading,
      searchRouteId: searchRouteId,
    );

    try {
      final matches = await _matching.findDriversFor(searchRouteId);
      if (state.searchRouteId != searchRouteId) return;
      state = state.copyWith(
        phase: PassengerMatchPhase.ready,
        matches: matches,
      );
    } catch (e) {
      if (state.searchRouteId != searchRouteId) return;
      state = state.copyWith(
        phase: PassengerMatchPhase.failed,
        errorMessage: authErrorMessage(e),
      );
    }
  }

  /// Pede a vaga no trajeto do motorista, na próxima data em comum.
  Future<bool> requestSeat(MatchModel match) async {
    if (state.isBusy) return false;

    final date = match.nextRideDate;
    if (date == null) {
      state = state.copyWith(
        notice: 'Não há uma data próxima em comum com este motorista.',
      );
      return false;
    }

    state = state.copyWith(
      phase: PassengerMatchPhase.requesting,
      selected: match,
      notice: null,
    );

    try {
      final ride = await _rides.requestSeat(
        driverRouteId: match.driverRouteId,
        passengerRouteId: match.passengerRouteId,
        date: date,
      );
      state = state.copyWith(
        phase: PassengerMatchPhase.waiting,
        rideId: ride.id,
      );
      _apply(ride);
      if (state.phase == PassengerMatchPhase.waiting) _startPolling();
      return true;
    } catch (e) {
      state = state.copyWith(
        phase: PassengerMatchPhase.ready,
        selected: null,
        notice: authErrorMessage(e),
      );
      return false;
    }
  }

  /// Consulta a viagem uma vez. Chamado pelo timer enquanto espera a resposta.
  Future<void> refreshRequest() async {
    final rideId = state.rideId;
    if (rideId == null || state.phase != PassengerMatchPhase.waiting) return;

    try {
      _apply(await _rides.getRide(rideId));
    } catch (_) {
      // Falha passageira de rede: tenta de novo na próxima volta do timer.
    }
  }

  /// Desiste do pedido enquanto o motorista não respondeu.
  Future<void> cancelRequest() async {
    final rideId = state.rideId;
    _stopPolling();

    if (rideId != null && state.phase == PassengerMatchPhase.waiting) {
      state = state.copyWith(isCancelling: true);
      try {
        await _rides.leaveRide(
          rideId,
          reason: 'Pedido cancelado pelo passageiro.',
        );
      } catch (e) {
        state = state.copyWith(notice: authErrorMessage(e));
      }
    }

    state = state.copyWith(
      phase: PassengerMatchPhase.ready,
      selected: null,
      rideId: null,
      isCancelling: false,
    );
  }

  /// O aviso já foi mostrado.
  void clearNotice() => state = state.copyWith(notice: null);

  /// Volta ao início (o passageiro fechou o fluxo).
  void reset() {
    _stopPolling();
    state = const PassengerMatchState();
  }

  void _apply(RideModel ride) {
    if (state.phase != PassengerMatchPhase.waiting || state.rideId != ride.id) {
      return;
    }

    final viewerId = ref.read(matchingViewerIdProvider);
    final status = viewerId == null ? null : ride.passengerOf(viewerId)?.status;

    switch (status) {
      case ParticipantStatus.confirmed || ParticipantStatus.completed:
        _stopPolling();
        state = state.copyWith(phase: PassengerMatchPhase.accepted);
      case ParticipantStatus.rejected || ParticipantStatus.cancelled:
        _stopPolling();
        final declined = state.selected;
        state = state.copyWith(
          phase: PassengerMatchPhase.ready,
          matches: [
            for (final match in state.matches)
              if (match.driverRouteId != declined?.driverRouteId) match,
          ],
          selected: null,
          rideId: null,
          notice:
              '${declined?.otherUserName ?? 'O motorista'} não pôde aceitar. Escolha outro motorista.',
        );
      default:
        break;
    }
  }

  void _startPolling() {
    _stopPolling();
    _polling = Timer.periodic(
      ref.read(matchingPollIntervalProvider),
      (_) => refreshRequest(),
    );
  }

  void _stopPolling() {
    _polling?.cancel();
    _polling = null;
  }
}
