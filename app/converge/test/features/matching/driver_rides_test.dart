// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/state/driver_ride_actions_controller.dart';
import 'package:converge/features/matching/state/driver_rides_provider.dart';
import 'package:converge/features/matching/state/matching_viewer_provider.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_seat_ride_repository.dart';

void main() {
  ProviderContainer containerWith(
    FakeSeatRideRepository rides, {
    String viewerId = fakeDriverId,
  }) {
    final container = ProviderContainer.test(
      overrides: [
        rideRepositoryProvider.overrideWithValue(rides),
        matchingViewerIdProvider.overrideWithValue(viewerId),
      ],
    );
    container.listen(myDrivenRidesProvider, (_, _) {});
    container.listen(driverRideInProgressProvider, (_, _) {});
    container.listen(driverRideActionsProvider, (_, _) {});
    return container;
  }

  test('a viagem em andamento é o expediente', () async {
    final container = containerWith(
      FakeSeatRideRepository(myRides: [fakeRide(status: 'IN_PROGRESS')]),
    );

    await container.read(myDrivenRidesProvider.future);

    expect(container.read(driverRideInProgressProvider)?.id, fakeRideId);
  });

  test(
    'iniciar e concluir chamam a API; um erro volta como mensagem',
    () async {
      final rides = FakeSeatRideRepository(
        myRides: [fakeRide(status: 'CONFIRMED')],
      );
      final container = containerWith(rides);
      final actions = container.read(driverRideActionsProvider.notifier);

      expect(await actions.start(fakeRideId), isNull);
      expect(await actions.complete(fakeRideId), isNull);
      expect(
        rides.calls,
        containsAllInOrder(['start:$fakeRideId', 'complete:$fakeRideId']),
      );

      rides.failNextWith = 'Só é possível iniciar uma viagem confirmada.';
      expect(
        await actions.start(fakeRideId),
        'Só é possível iniciar uma viagem confirmada.',
      );
      expect(container.read(driverRideActionsProvider), isEmpty);
    },
  );
}
