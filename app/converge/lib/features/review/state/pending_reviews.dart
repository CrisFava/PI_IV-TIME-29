// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter/foundation.dart';

import '../../ride/data/models/participant_status.dart';
import '../../ride/data/models/ride_model.dart';
import '../../ride/data/models/ride_status.dart';
import '../data/models/review_model.dart';

/// Pessoa que o usuário ainda pode avaliar em uma viagem concluída.
@immutable
class ReviewTarget {
  final String userId;
  final String name;

  /// `true` quando quem será avaliado é o motorista.
  final bool isDriver;

  const ReviewTarget({
    required this.userId,
    required this.name,
    required this.isDriver,
  });
}

/// Quem [viewerId] ainda não avaliou na viagem [ride]:
/// o passageiro avalia o motorista; o motorista avalia cada passageiro que viajou.
/// Quem faltou, cancelou ou saiu antes da conclusão não entra, e quem já foi avaliado também não.
List<ReviewTarget> pendingReviewTargets({
  required RideModel ride,
  required String viewerId,
  required List<ReviewModel> given,
}) {
  // Só viagem concluída tem avaliação.
  if (ride.status != RideStatus.completed) return const [];

  final reviewed = {for (final review in given) review.reviewedId};

  // Motorista: cada passageiro que viajou e que ele ainda não avaliou.
  if (ride.isDrivenBy(viewerId)) {
    return [
      for (final passenger in ride.participants)
        if (passenger.isPassenger &&
            passenger.status == ParticipantStatus.completed &&
            passenger.userId != null &&
            !reviewed.contains(passenger.userId))
          ReviewTarget(
            userId: passenger.userId!,
            name: passenger.displayName,
            isDriver: false,
          ),
    ];
  }

  // Passageiro: só avalia se viajou, e só o motorista, se ainda não avaliou.
  final own = ride.passengerOf(viewerId);
  if (own == null || own.status != ParticipantStatus.completed) return const [];
  if (reviewed.contains(ride.driverId)) return const [];

  return [
    ReviewTarget(userId: ride.driverId, name: ride.driverName, isDriver: true),
  ];
}

/// Viagens concluídas há pouco ([within]), para lembrar de avaliar quando o app abre e
/// a pessoa não estava olhando na hora em que a viagem terminou (o passageiro, em geral).
List<RideModel> recentlyCompletedRides({
  required List<RideModel> rides,
  required DateTime now,
  Duration within = const Duration(hours: 24),
}) => [
  for (final ride in rides)
    if (ride.status == RideStatus.completed &&
        ride.completedAt != null &&
        now.difference(ride.completedAt!) <= within)
      ride,
];

/// Viagens que viraram concluídas entre a lista [previous] e a lista [next].
/// Sem lista anterior (primeira carga do app) não há nenhuma: a tela de avaliação
/// só abre sozinha no momento em que a viagem termina, não por viagens antigas.
List<RideModel> newlyCompletedRides({
  required List<RideModel>? previous,
  required List<RideModel> next,
}) {
  if (previous == null) return const [];

  final before = {for (final ride in previous) ride.id: ride.status};
  return [
    for (final ride in next)
      if (ride.status == RideStatus.completed &&
          before[ride.id] != null &&
          before[ride.id] != RideStatus.completed)
        ride,
  ];
}
