// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/participant_status.dart';
import '../../ride/data/models/ride_model.dart';
import '../../ride/data/models/ride_status.dart';
import '../../ride/state/ride_providers.dart';
import 'matching_viewer_provider.dart';

const _upcomingStatuses = {
  RideStatus.awaitingConfirmation,
  RideStatus.confirmed,
  RideStatus.inProgress,
};

/// Viagens em que o usuário logado vai como passageiro, já aceito pelo motorista,
/// e que ainda não terminaram. Aparecem em "Minhas caronas" com a etiqueta
/// "Passageiro". Recarregue com `ref.invalidate(myRidesProvider)`.
final myPassengerRidesProvider = FutureProvider.autoDispose<List<RideModel>>((
  ref,
) async {
  final viewerId = ref.watch(matchingViewerIdProvider);
  if (viewerId == null) return const [];

  final rides = await ref.watch(myRidesProvider.future);
  final asPassenger = [
    for (final ride in rides)
      if (!ride.isDrivenBy(viewerId) &&
          _upcomingStatuses.contains(ride.status) &&
          ride.passengerOf(viewerId)?.status == ParticipantStatus.confirmed)
        ride,
  ];
  asPassenger.sort(
    (a, b) => (a.departureDateTime ?? DateTime(9999)).compareTo(
      b.departureDateTime ?? DateTime(9999),
    ),
  );
  return asPassenger;
});
