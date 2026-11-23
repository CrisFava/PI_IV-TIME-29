// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/data/models/driver_ride_request.dart';
import 'package:converge/features/matching/state/driver_ride_requests_provider.dart';
import 'package:converge/features/matching/state/matching_viewer_provider.dart';
import 'package:converge/features/matching/state/ride_request_actions_controller.dart';
import 'package:converge/features/ride/data/models/participant_status.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_seat_ride_repository.dart';

/// Testes unitários e de widgets para driver ride requests.
/// Cobre cenários como: pedidos que o motorista pode aceitar.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
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

  group('pedidos que o motorista pode aceitar', () {
    // Uma viagem com três pedidos: Carla pediu primeiro, depois João e Ana
    List<DriverRideRequest> requestsOf({required int availableSeats}) {
      final json = fakeRideJson(
        status: 'AWAITING_CONFIRMATION',
        passengerStatus: 'REQUESTED',
        extraParticipants: [
          {
            'userId': 'passenger-2',
            'name': 'João Pereira',
            'role': 'PASSENGER',
            'status': 'REQUESTED',
            'requestedAt': '2026-11-16T06:50:00',
          },
          {
            'userId': 'passenger-3',
            'name': 'Carla Dias',
            'role': 'PASSENGER',
            'status': 'REQUESTED',
            'requestedAt': '2026-11-16T06:40:00',
          },
        ],
      )..['availableSeats'] = availableSeats;
      (json['participants'] as List)[1]['requestedAt'] = '2026-11-16T07:00:00';
      final ride = RideModel.fromJson(json);
      return [
        for (final participant in ride.participants)
          if (participant.isPassenger)
            DriverRideRequest(ride: ride, passenger: participant),
      ];
    }

    test('vêm do pedido mais antigo para o mais novo', () {
      final names = pendingRideRequests(requestsOf(availableSeats: 2))
          .map((r) => r.passenger.displayName);

      expect(names, ['Carla Dias', 'João Pereira', 'Ana Souza']);
    });

    test('somem com o carro cheio e voltam quando abre uma vaga', () {
      expect(pendingRideRequests(requestsOf(availableSeats: 0)), isEmpty);
      expect(pendingRideRequests(requestsOf(availableSeats: 1)), hasLength(3));
    });

    test('a busca acha o passageiro pelo nome, sem ligar para acento', () {
      final found = pendingRideRequests(
        requestsOf(availableSeats: 2),
        query: 'joao',
      );

      expect(found.single.passenger.displayName, 'João Pereira');
    });
  });
}
