// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/participant_status.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../route/data/models/route_response_model.dart';

/// Uma carona que o motorista oferece: o trajeto de oferta publicado e as viagens
/// que já saíram dele (uma por data em que alguém pediu vaga).
/// Sem [route], é uma viagem cuja oferta foi cancelada mas que ainda tem passageiros.
@immutable
class DriverOffer {
  final RouteResponseModel? route;
  final List<RideModel> rides;

  DriverOffer({required this.route, required List<RideModel> rides})
    : rides = List.unmodifiable(rides);

  String get key => route?.id ?? 'ride:${rides.first.id}';

  /// A viagem em andamento ou, se não houver, a mais próxima com passageiro
  /// confirmado: é a que o motorista inicia ou conclui direto do card.
  RideModel? get actionableRide {
    for (final ride in rides) {
      if (ride.status == RideStatus.inProgress) return ride;
    }
    RideModel? next;
    for (final ride in rides) {
      final departure = ride.departureDateTime;
      if (_count(ride, ParticipantStatus.confirmed) == 0 || departure == null) {
        continue;
      }
      final nextDeparture = next?.departureDateTime;
      if (nextDeparture == null || departure.isBefore(nextDeparture)) {
        next = ride;
      }
    }
    return next;
  }

  /// Passageiros confirmados em todas as viagens da carona.
  int get confirmedPassengers =>
      rides.fold(0, (sum, r) => sum + _count(r, ParticipantStatus.confirmed));

  /// Pedidos que dá para aceitar agora: os das viagens com vaga (carro cheio
  /// não conta). É o número da bolinha vermelha do card.
  int get openRequests => rides
      .where((r) => r.availableSeats > 0)
      .fold(0, (sum, r) => sum + _count(r, ParticipantStatus.requested));

  /// Viagens ainda não iniciadas que dá para cancelar.
  List<RideModel> get openRides => [
    for (final ride in rides)
      if (ride.status == RideStatus.awaitingConfirmation ||
          ride.status == RideStatus.confirmed)
        ride,
  ];

  bool get hasOpenRides => openRides.isNotEmpty;

  static int _count(RideModel ride, ParticipantStatus status) => ride
      .participants
      .where((p) => p.isPassenger && p.status == status)
      .length;
}
