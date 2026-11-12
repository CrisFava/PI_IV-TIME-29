// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/ride_model.dart';
import '../data/models/ride_passenger_ref.dart';
import '../data/models/ride_payment_model.dart';
import '../data/repository/ride_repository_impl.dart';

/// Viagens em que o usuário participa. Recarregue com `ref.invalidate(myRidesProvider)`.
final myRidesProvider = FutureProvider.autoDispose<List<RideModel>>(
  (ref) => ref.watch(rideRepositoryProvider).listMyRides(),
);

/// Uma viagem pelo id. Recarregue com `ref.invalidate(rideProvider(rideId))`.
final rideProvider = FutureProvider.autoDispose.family<RideModel, String>(
  (ref, rideId) => ref.watch(rideRepositoryProvider).getRide(rideId),
);

/// O pagamento de um passageiro em uma viagem.
final ridePaymentProvider = FutureProvider.autoDispose
    .family<RidePaymentModel, RidePassengerRef>(
      (ref, passenger) => ref
          .watch(rideRepositoryProvider)
          .getPayment(passenger.rideId, passenger.passengerId),
    );
