// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/data/repository/matching_repository_impl.dart';
import 'package:converge/features/matching/state/matching_viewer_provider.dart';
import 'package:converge/features/matching/state/passenger_match_controller.dart';
import 'package:converge/features/matching/state/passenger_match_phase.dart';
import 'package:converge/features/matching/state/passenger_match_state.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/route/data/models/create_route_request.dart';
import 'package:converge/features/route/data/models/route_place_model.dart';
import 'package:converge/features/route/data/repository/route_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_matches.dart';
import 'fakes/fake_matching_repository.dart';
import 'fakes/fake_matching_route_repository.dart';
import 'fakes/fake_seat_ride_repository.dart';

void main() {
  late FakeMatchingRepository matching;
  late FakeSeatRideRepository rides;
  late FakeMatchingRouteRepository routes;
  late ProviderContainer container;

  PassengerMatchController controller() =>
      container.read(passengerMatchProvider.notifier);
  PassengerMatchState state() => container.read(passengerMatchProvider);
  List<String> driverNames() => [
    for (final m in state().matches) m.otherUserName,
  ];

  setUp(() {
    matching = FakeMatchingRepository(
      drivers: [
        fakeMatch(),
        fakeMatch(
          driverRouteId: 'route-2',
          driverName: 'Mariana Costa',
          score: 0.8,
        ),
      ],
    );
    rides = FakeSeatRideRepository();
    routes = FakeMatchingRouteRepository();
    container = ProviderContainer.test(
      overrides: [
        matchingRepositoryProvider.overrideWithValue(matching),
        rideRepositoryProvider.overrideWithValue(rides),
        routeRepositoryProvider.overrideWithValue(routes),
        matchingViewerIdProvider.overrideWithValue(fakePassengerId),
        // O teste consulta a viagem na mão: o timer não pode disparar sozinho.
        matchingPollIntervalProvider.overrideWithValue(
          const Duration(hours: 1),
        ),
      ],
    );
    container.listen(passengerMatchProvider, (_, _) {});
  });

  group('findDrivers', () {
    test('carrega os motoristas compatíveis do trajeto', () async {
      await controller().findDrivers(fakeSearchRouteId);

      expect(state().phase, PassengerMatchPhase.ready);
      expect(driverNames(), ['Lucas Silveira', 'Mariana Costa']);
      expect(matching.calls, ['findDriversFor:$fakeSearchRouteId']);
    });

    test('mostra o erro quando a busca falha', () async {
      matching.failNextWith =
          'Só é possível buscar motoristas com um trajeto de procura.';

      await controller().findDrivers(fakeSearchRouteId);

      expect(state().phase, PassengerMatchPhase.failed);
      expect(
        state().errorMessage,
        'Só é possível buscar motoristas com um trajeto de procura.',
      );
    });
  });

  group('aviso do motorista disponível', () {
    test('recusar sugere o próximo e, sem mais nenhum, o aviso some', () async {
      await controller().findDrivers(fakeSearchRouteId);

      controller().declineSuggestion(state().suggestion!);
      expect(state().suggestion?.otherUserName, 'Mariana Costa');

      controller().declineSuggestion(state().suggestion!);
      expect(state().suggestion, isNull);
      expect(state().matches, hasLength(2));
    });

  });

  group('requestSeat', () {
    setUp(() => controller().findDrivers(fakeSearchRouteId));

    test('pede a vaga na próxima data em comum e passa a esperar', () async {
      final requested = await controller().requestSeat(fakeMatch());

      expect(requested, isTrue);
      expect(state().phase, PassengerMatchPhase.waiting);
      expect(state().rideId, fakeRideId);
      expect(state().selected?.driverRouteId, 'route-1');
      expect(rides.calls, [
        'requestSeat:route-1:$fakeSearchRouteId:2026-11-16',
      ]);
    });

    test('quando o motorista aceita, o chat fica liberado', () async {
      await controller().requestSeat(fakeMatch());
      rides.passengerStatus = 'CONFIRMED';

      await controller().refreshRequest();

      expect(state().phase, PassengerMatchPhase.accepted);
    });

    test(
      'quando o motorista recusa, volta para a lista sem ele e avisa',
      () async {
        await controller().requestSeat(fakeMatch());
        rides.passengerStatus = 'REJECTED';

        await controller().refreshRequest();

        expect(state().phase, PassengerMatchPhase.ready);
        expect(driverNames(), ['Mariana Costa']);
        expect(state().rideId, isNull);
        expect(state().notice, contains('Lucas Silveira não pôde aceitar'));
      },
    );

    test('enquanto o motorista não responde, continua esperando', () async {
      await controller().requestSeat(fakeMatch());

      await controller().refreshRequest();

      expect(state().phase, PassengerMatchPhase.waiting);
    });

    test('sem data em comum, nem envia o pedido', () async {
      final requested = await controller().requestSeat(
        fakeMatch(nextRideDate: null),
      );

      expect(requested, isFalse);
      expect(rides.calls, isEmpty);
      expect(state().notice, isNotNull);
    });

    test(
      'se o pedido falha, volta para a lista com a mensagem do backend',
      () async {
        rides.failNextWith = 'Você não pode pedir vaga no seu próprio trajeto.';

        final requested = await controller().requestSeat(fakeMatch());

        expect(requested, isFalse);
        expect(state().phase, PassengerMatchPhase.ready);
        expect(
          state().notice,
          'Você não pode pedir vaga no seu próprio trajeto.',
        );
      },
    );

    test('não envia dois pedidos ao mesmo tempo', () async {
      await controller().requestSeat(fakeMatch());

      final second = await controller().requestSeat(
        fakeMatch(driverRouteId: 'route-2'),
      );

      expect(second, isFalse);
      expect(
        rides.calls.where((c) => c.startsWith('requestSeat')),
        hasLength(1),
      );
    });

    test('cancelar o pedido sai da viagem e volta para a lista', () async {
      await controller().requestSeat(fakeMatch());

      await controller().cancelRequest();

      expect(rides.calls.last, 'leaveRide:$fakeRideId');
      expect(state().phase, PassengerMatchPhase.ready);
      expect(state().rideId, isNull);
      expect(state().isCancelling, isFalse);
    });
  });

  group('prepareSearchRoute', () {
    final request = CreateRouteRequest(
      type: AppRouteType.search,
      origin: const RoutePlaceModel(
        address: 'Rua das Flores, 50',
        city: 'Campinas',
        latitude: -22.9064,
        longitude: -47.0616,
      ),
      destination: const RoutePlaceModel(
        address: 'PUC-Campinas',
        city: 'Campinas',
        latitude: -22.8338,
        longitude: -47.0519,
      ),
      departureTime: '08:00',
      estimatedArrivalTime: '08:45',
      weekDays: const {'MONDAY'},
      totalSeats: 1,
    );

    test('cria o trajeto de procura quando ainda não existe', () async {
      final id = await controller().prepareSearchRoute(request);

      expect(id, 'created-1');
      expect(routes.calls, ['listMyRoutes', 'createRoute']);
    });

    test('reaproveita um trajeto igual já salvo', () async {
      routes.routes.add(
        FakeMatchingRouteRepository.routeFrom('saved-1', request),
      );

      final id = await controller().prepareSearchRoute(request);

      expect(id, 'saved-1');
      expect(routes.calls, ['listMyRoutes']);
    });

    test('se outro pedido salvou o mesmo trajeto antes (409), usa o que ficou salvo', () async {
      routes.conflictOnCreate = true;

      final id = await controller().prepareSearchRoute(request);

      expect(id, 'saved-elsewhere');
    });

    test('fechar o pedido desativa o trajeto criado só para ele', () async {
      await controller().prepareSearchRoute(request);

      controller().reset();
      await pumpEventQueue();

      expect(routes.calls, contains('deleteRoute:created-1'));
    });

    test('com a carona aceita, o trajeto de procura continua', () async {
      final searchId = await controller().prepareSearchRoute(request);
      await controller().findDrivers(searchId!);
      await controller().requestSeat(fakeMatch());
      rides.passengerStatus = 'CONFIRMED';
      await controller().refreshRequest();
      expect(state().phase, PassengerMatchPhase.accepted);

      controller().reset();
      await pumpEventQueue();

      expect(routes.calls, isNot(contains('deleteRoute:created-1')));
    });
  });
}
