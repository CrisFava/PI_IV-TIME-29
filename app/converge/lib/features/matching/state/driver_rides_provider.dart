// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/ride_model.dart';
import '../../ride/data/models/ride_status.dart';
import '../../ride/state/ride_providers.dart';
import 'matching_viewer_provider.dart';

const _upcomingStatuses = {
  RideStatus.awaitingConfirmation,
  RideStatus.confirmed,
  RideStatus.inProgress,
};

/// Viagens que o motorista logado ainda vai fazer (ou está fazendo), da mais
/// próxima para a mais distante. Recarregue com `ref.invalidate(myRidesProvider)`.
final myDrivenRidesProvider = FutureProvider.autoDispose<List<RideModel>>((
  ref,
) async {
  final viewerId = ref.watch(matchingViewerIdProvider);
  if (viewerId == null) return const [];

  final rides = await ref.watch(myRidesProvider.future);
  final driven = [
    for (final ride in rides)
      if (ride.isDrivenBy(viewerId) && _upcomingStatuses.contains(ride.status))
        ride,
  ];
  driven.sort((a, b) {
    final aDate = a.departureDateTime ?? DateTime(9999);
    final bDate = b.departureDateTime ?? DateTime(9999);
    return aDate.compareTo(bDate);
  });
  return driven;
});

/// Viagem em andamento do motorista logado: é o expediente (ou `null`).
final driverRideInProgressProvider = Provider.autoDispose<RideModel?>((ref) {
  final rides = ref.watch(myDrivenRidesProvider).value ?? const [];
  for (final ride in rides) {
    if (ride.status == RideStatus.inProgress) return ride;
  }
  return null;
});
