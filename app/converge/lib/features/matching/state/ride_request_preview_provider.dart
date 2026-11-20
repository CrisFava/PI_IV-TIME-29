// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/participant_status.dart';
import '../../ride/data/models/ride_preview_model.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import 'driver_rides_provider.dart';

/// Como a viagem ficaria aceitando o pedido de um passageiro: os dois caminhos e o desvio.
/// Refaz a simulação quando muda quem está confirmado na viagem.
final rideRequestPreviewProvider = FutureProvider.autoDispose
    .family<RidePreviewModel, ({String rideId, String passengerId})>((
      ref,
      key,
    ) {
      ref.watch(
        myDrivenRidesProvider.select(
          (rides) => [
            for (final ride in rides.value ?? const [])
              if (ride.id == key.rideId)
                for (final p in ride.participants)
                  if (p.isPassenger && p.status == ParticipantStatus.confirmed)
                    p.userId,
          ].join(','),
        ),
      );
      return ref
          .read(rideRepositoryProvider)
          .previewPassenger(key.rideId, key.passengerId);
    });
