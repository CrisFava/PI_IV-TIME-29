// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../data/models/match_model.dart';
import 'passenger_match_phase.dart';

/// Estado do passageiro: motoristas compatíveis e o pedido de vaga em andamento.
@immutable
class PassengerMatchState {
  static const Object _unset = Object();

  final PassengerMatchPhase phase;
  final String? searchRouteId;
  final List<MatchModel> matches;

  /// Motorista escolhido para o pedido em andamento.
  final MatchModel? selected;

  /// Viagem criada pelo pedido de vaga.
  final String? rideId;

  /// Cancelando o pedido (desabilita o botão).
  final bool isCancelling;
  final String? errorMessage;

  /// Aviso pontual para a tela (ex.: o motorista recusou).
  final String? notice;

  /// Motoristas que o passageiro recusou no aviso: não são sugeridos de novo nesta busca.
  final Set<String> declinedRouteIds;

  const PassengerMatchState({
    this.phase = PassengerMatchPhase.idle,
    this.searchRouteId,
    this.matches = const [],
    this.selected,
    this.rideId,
    this.isCancelling = false,
    this.errorMessage,
    this.notice,
    this.declinedRouteIds = const {},
  });

  /// Motorista sugerido no aviso: o mais compatível que ainda dá para pedir e que
  /// o passageiro não recusou. A lista já vem do mais para o menos compatível.
  MatchModel? get suggestion {
    if (phase != PassengerMatchPhase.ready) return null;
    for (final match in matches) {
      if (match.canRequestSeat &&
          !declinedRouteIds.contains(match.driverRouteId)) {
        return match;
      }
    }
    return null;
  }

  bool get isBusy =>
      phase == PassengerMatchPhase.requesting ||
      phase == PassengerMatchPhase.waiting ||
      isCancelling;

  PassengerMatchState copyWith({
    PassengerMatchPhase? phase,
    Object? searchRouteId = _unset,
    List<MatchModel>? matches,
    Object? selected = _unset,
    Object? rideId = _unset,
    bool? isCancelling,
    Object? errorMessage = _unset,
    Object? notice = _unset,
    Set<String>? declinedRouteIds,
  }) {
    return PassengerMatchState(
      phase: phase ?? this.phase,
      searchRouteId: identical(searchRouteId, _unset)
          ? this.searchRouteId
          : searchRouteId as String?,
      matches: matches ?? this.matches,
      selected: identical(selected, _unset)
          ? this.selected
          : selected as MatchModel?,
      rideId: identical(rideId, _unset) ? this.rideId : rideId as String?,
      isCancelling: isCancelling ?? this.isCancelling,
      errorMessage: identical(errorMessage, _unset)
          ? this.errorMessage
          : errorMessage as String?,
      notice: identical(notice, _unset) ? this.notice : notice as String?,
      declinedRouteIds: declinedRouteIds ?? this.declinedRouteIds,
    );
  }
}
