// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:math' as math;

import 'package:flutter/foundation.dart';

import '../../route/data/models/route_place_model.dart';
import '../data/models/participant_status.dart';
import '../data/models/ride_model.dart';
import '../data/models/ride_participant_model.dart';
import '../data/models/ride_stop_type.dart';

/// Uma etapa da viagem em andamento: uma parada, ou várias seguidas do mesmo tipo
/// no mesmo lugar (ex.: três passageiros descendo na PUC).
@immutable
class TripStep {
  final RideStopType type;
  final RoutePlaceModel place;

  /// Os passageiros desta etapa, com a situação de cada um.
  final List<RideParticipantModel> passengers;

  /// Posição da primeira e da última parada desta etapa no itinerário.
  final int firstStop;
  final int lastStop;

  final bool done;

  const TripStep({
    required this.type,
    required this.place,
    required this.passengers,
    required this.firstStop,
    required this.lastStop,
    required this.done,
  });

  bool get isPickup => type == RideStopType.pickup;
}

/// Onde a viagem está: as etapas na ordem e qual é a próxima. Sem localização real,
/// uma etapa termina quando o motorista confirma quem entrou ou desceu.
@immutable
class TripProgress {
  final List<TripStep> steps;

  /// A primeira etapa ainda não feita; igual a `steps.length` quando todas acabaram.
  final int currentIndex;

  const TripProgress({required this.steps, required this.currentIndex});

  TripStep? get current =>
      currentIndex < steps.length ? steps[currentIndex] : null;

  /// Todas as paradas feitas: falta só chegar ao destino e concluir.
  bool get finished => current == null;

  /// O caminho em trechos: `legs[i]` termina na parada `i` e o último, no destino.
  /// Devolve quais trechos formam o trecho atual (até a próxima etapa) e quais vêm depois.
  ({List<int> current, List<int> upcoming}) legsFor(int legCount) {
    final step = current;
    final from = step?.firstStop ?? legCount - 1;
    final to = step?.lastStop ?? legCount - 1;
    return (
      current: [for (var i = from; i <= to && i < legCount; i++) i],
      upcoming: [for (var i = to + 1; i < legCount; i++) i],
    );
  }

  static TripProgress of(RideModel ride) {
    final steps = <TripStep>[];
    final stops = ride.itinerary;

    for (var i = 0; i < stops.length; i++) {
      final stop = stops[i];
      final passenger = ride.passengerOf(stop.passengerId);
      final previous = steps.isEmpty ? null : steps.last;

      // Mesma ação, logo em seguida e no mesmo lugar: entra na etapa anterior
      if (previous != null &&
          previous.type == stop.type &&
          previous.lastStop == i - 1 &&
          sameSpot(previous.place, stop.place)) {
        final passengers = [...previous.passengers, ?passenger];
        steps.last = TripStep(
          type: previous.type,
          place: previous.place,
          passengers: passengers,
          firstStop: previous.firstStop,
          lastStop: i,
          done: previous.done && _stopDone(stop.type, passenger),
        );
        continue;
      }

      steps.add(
        TripStep(
          type: stop.type,
          place: stop.place,
          passengers: [?passenger],
          firstStop: i,
          lastStop: i,
          done: _stopDone(stop.type, passenger),
        ),
      );
    }

    final current = steps.indexWhere((step) => !step.done);
    return TripProgress(
      steps: List.unmodifiable(steps),
      currentIndex: current < 0 ? steps.length : current,
    );
  }

  /// Embarque feito: o passageiro entrou, desceu, não apareceu ou saiu da viagem.
  /// Desembarque feito: ele não está mais a bordo (desceu, não apareceu ou saiu).
  static bool _stopDone(RideStopType type, RideParticipantModel? passenger) {
    if (passenger == null) return true;
    final stillConfirmed = passenger.status == ParticipantStatus.confirmed;
    if (type == RideStopType.pickup) {
      return !stillConfirmed || passenger.boardedAt != null;
    }
    return !stillConfirmed;
  }

  /// Até uns 50 m: é o mesmo ponto de parada.
  static bool sameSpot(RoutePlaceModel a, RoutePlaceModel b) {
    const metersPerDegree = 111320.0;
    final dLat = (a.latitude - b.latitude) * metersPerDegree;
    final dLng =
        (a.longitude - b.longitude) *
        metersPerDegree *
        math.cos(a.latitude * math.pi / 180);
    return math.sqrt(dLat * dLat + dLng * dLng) <= 50;
  }
}
