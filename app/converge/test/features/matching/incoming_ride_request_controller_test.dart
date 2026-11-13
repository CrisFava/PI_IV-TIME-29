// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/state/incoming_ride_request_controller.dart';
import 'package:converge/features/matching/state/matching_viewer_provider.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_seat_ride_repository.dart';

void main() {
  ProviderContainer containerWith(FakeSeatRideRepository rides) {
    final container = ProviderContainer.test(
      overrides: [
        rideRepositoryProvider.overrideWithValue(rides),
        matchingViewerIdProvider.overrideWithValue(fakeDriverId),
        // O teste controla as consultas: o timer não pode disparar sozinho
        matchingPollIntervalProvider.overrideWithValue(
          const Duration(hours: 1),
        ),
      ],
    );
    container.listen(incomingRideRequestProvider, (_, _) {});
    return container;
  }

  Future<void> settle() => pumpEventQueue();

  test('mostra o pedido pendente, com o embarque do passageiro', () async {
    final container = containerWith(
      FakeSeatRideRepository(
        myRides: [
          fakeRide(
            status: 'AWAITING_CONFIRMATION',
            passengerStatus: 'REQUESTED',
          ),
        ],
      ),
    );
    await settle();

    final request = container.read(incomingRideRequestProvider);
    expect(request?.passenger.displayName, 'Ana Souza');
    expect(
      request?.passenger.boardingLocation?.address,
      'Rua Coronel Quirino, 900',
    );
  });

  test(
    'dispensar passa para o próximo pedido e, sem mais nenhum, some',
    () async {
      final container = containerWith(
        FakeSeatRideRepository(
          myRides: [
            fakeRide(
              status: 'AWAITING_CONFIRMATION',
              passengerStatus: 'REQUESTED',
              extraParticipants: [
                {
                  'userId': 'passenger-2',
                  'name': 'Bruno Lima',
                  'role': 'PASSENGER',
                  'status': 'REQUESTED',
                },
              ],
            ),
          ],
        ),
      );
      await settle();
      final controller = container.read(incomingRideRequestProvider.notifier);

      final first = container.read(incomingRideRequestProvider)!;
      expect(first.passenger.displayName, 'Ana Souza');

      controller.dismiss(first);
      final second = container.read(incomingRideRequestProvider)!;
      expect(second.passenger.displayName, 'Bruno Lima');

      controller.dismiss(second);
      expect(container.read(incomingRideRequestProvider), isNull);
    },
  );

}
