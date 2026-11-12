// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/participant_status.dart';
import '../../ride/data/models/ride_status.dart';
import '../../ride/state/ride_providers.dart';
import '../data/models/driver_ride_request.dart';
import 'matching_viewer_provider.dart';

const _openRideStatuses = {
  RideStatus.awaitingConfirmation,
  RideStatus.confirmed,
  RideStatus.inProgress,
};

/// Passageiros nas viagens em aberto que o usuário logado dirige: os pedidos
/// esperando resposta e os já aceitos. Recarregue com `ref.invalidate(myRidesProvider)`.
final driverRideRequestsProvider =
    FutureProvider.autoDispose<List<DriverRideRequest>>((ref) async {
      final viewerId = ref.watch(matchingViewerIdProvider);
      if (viewerId == null) return const [];

      final rides = await ref.watch(myRidesProvider.future);
      return [
        for (final ride in rides)
          if (ride.isDrivenBy(viewerId) &&
              _openRideStatuses.contains(ride.status))
            for (final participant in ride.participants)
              if (participant.isPassenger &&
                  participant.userId != null &&
                  (participant.status == ParticipantStatus.requested ||
                      participant.status.hasSeat))
                DriverRideRequest(ride: ride, passenger: participant),
      ];
    });
