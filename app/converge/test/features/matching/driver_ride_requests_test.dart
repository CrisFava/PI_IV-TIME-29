// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/state/driver_ride_requests_provider.dart';
import 'package:converge/features/matching/state/matching_viewer_provider.dart';
import 'package:converge/features/matching/state/ride_request_actions_controller.dart';
import 'package:converge/features/ride/data/models/participant_status.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_seat_ride_repository.dart';

void main() {
  late FakeSeatRideRepository rides;
  late ProviderContainer container;

  ProviderContainer containerFor(String viewerId) {
    final created = ProviderContainer.test(
      overrides: [
        rideRepositoryProvider.overrideWithValue(rides),
        matchingViewerIdProvider.overrideWithValue(viewerId),
      ],
    );
    created.listen(driverRideRequestsProvider, (_, _) {});
    created.listen(rideRequestActionsProvider, (_, _) {});
    return created;
  }

  setUp(() {
    rides = FakeSeatRideRepository(
      myRides: [
        fakeRide(
          status: 'AWAITING_CONFIRMATION',
          passengerStatus: 'REQUESTED',
          extraParticipants: [
            {
              'userId': 'passenger-2',
              'name': 'Bruno Lima',
              'role': 'PASSENGER',
              'status': 'REJECTED',
            },
          ],
        ),
        fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED'),
      ],
    );
    container = containerFor(fakeDriverId);
  });

  test(
    'lista os pedidos das viagens em aberto que o motorista dirige',
    () async {
      final requests = await container.read(driverRideRequestsProvider.future);

      expect(requests, hasLength(1));
      expect(requests.single.passenger.displayName, 'Ana Souza');
      expect(requests.single.passenger.status, ParticipantStatus.requested);
      expect(requests.single.key, '$fakeRideId:$fakePassengerId');
    },
  );

  test('um passageiro não vê pedidos de viagens que não dirige', () async {
    final asPassenger = containerFor(fakePassengerId);

    expect(await asPassenger.read(driverRideRequestsProvider.future), isEmpty);
  });

  test('aceitar chama a API e recarrega as viagens', () async {
    final request = (await container.read(driverRideRequestsProvider.future))
        .single;

    final error = await container
        .read(rideRequestActionsProvider.notifier)
        .accept(request);

    expect(error, isNull);
    expect(rides.calls, contains('accept:$fakeRideId:$fakePassengerId'));
    expect(container.read(rideRequestActionsProvider), isEmpty);
  });

  test('recusar devolve a mensagem do backend quando falha', () async {
    final request = (await container.read(driverRideRequestsProvider.future))
        .single;
    rides.failNextWith = 'O pedido já foi respondido.';

    final error = await container
        .read(rideRequestActionsProvider.notifier)
        .reject(request);

    expect(error, 'O pedido já foi respondido.');
    expect(container.read(rideRequestActionsProvider), isEmpty);
  });
}
