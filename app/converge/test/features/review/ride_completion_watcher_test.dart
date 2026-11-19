// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/review/state/ride_completion_watcher.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/models/ride_status.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/ride/state/ride_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import '../ride/fakes/fake_ride_repository.dart';

void main() {
  group('viagem a caminho', () {
    // fakeRide sai em 16/11/2026 às 07:30
    final sameDay = DateTime(2026, 11, 16, 6);

    test('em andamento sempre conta', () {
      expect(isRideOnItsWay(fakeRide(status: 'IN_PROGRESS'), sameDay), isTrue);
    });

    test('confirmada para hoje, ou para um dia que já passou, conta', () {
      final ride = fakeRide(status: 'CONFIRMED');

      expect(isRideOnItsWay(ride, sameDay), isTrue);
      expect(isRideOnItsWay(ride, DateTime(2026, 11, 18)), isTrue);
    });

    test('confirmada para as próximas 24 horas conta, mesmo sendo amanhã', () {
      final ride = fakeRide(status: 'CONFIRMED');

      // Na véspera, às 9h: a saída de amanhã às 07:30 está a menos de 24 horas
      expect(isRideOnItsWay(ride, DateTime(2026, 11, 15, 9)), isTrue);
    });

    test('confirmada para depois das próximas 24 horas não conta', () {
      final ride = fakeRide(status: 'CONFIRMED');

      expect(isRideOnItsWay(ride, DateTime(2026, 11, 15, 7)), isFalse);
      expect(isRideOnItsWay(ride, DateTime(2026, 11, 10)), isFalse);
    });

    test('concluída ou cancelada não conta', () {
      expect(
        isRideOnItsWay(
          fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED'),
          sameDay,
        ),
        isFalse,
      );
      expect(isRideOnItsWay(fakeRide(status: 'CANCELLED'), sameDay), isFalse);
    });
  });

  group('mudança de status', () {
    final inProgress = fakeRide(status: 'IN_PROGRESS');

    test('mesma situação não é mudança, em qualquer ordem', () {
      expect(rideStatusesChanged([inProgress], [inProgress]), isFalse);
      expect(rideStatusesChanged(const [], const []), isFalse);
    });

    test('viagem que muda de status é mudança', () {
      final completed = fakeRide(
        status: 'COMPLETED',
        passengerStatus: 'COMPLETED',
      );

      expect(rideStatusesChanged([inProgress], [completed]), isTrue);
    });

    test(
      'chegada de um passageiro é mudança, mesmo com a viagem em andamento',
      () {
        final arrived = fakeRide(
          status: 'IN_PROGRESS',
          passengerStatus: 'COMPLETED',
        );

        expect(rideStatusesChanged([inProgress], [arrived]), isTrue);
      },
    );

    test('mudança apenas no pagamento também é mudança', () {
      final json = fakeRideJson(status: 'IN_PROGRESS', passengerStatus: 'COMPLETED');
      final before = RideModel.fromJson(json);
      (json['participants'] as List)[1]['paymentStatus'] = 'PAID';
      final after = RideModel.fromJson(json);

      expect(rideStatusesChanged([before], [after]), isTrue);
      expect(rideStatusesChanged([after], [after]), isFalse);
    });

    test('viagem que aparece ou some é mudança', () {
      expect(rideStatusesChanged(const [], [inProgress]), isTrue);
      expect(rideStatusesChanged([inProgress], const []), isTrue);
    });
  });

  group('vigia da conclusão', () {
    late FakeRideRepository rides;
    late ProviderContainer container;

    ProviderContainer containerFor(FakeRideRepository repository) =>
        ProviderContainer.test(
          overrides: [
            rideRepositoryProvider.overrideWithValue(repository),
            rideCompletionPollIntervalProvider.overrideWithValue(
              const Duration(milliseconds: 40),
            ),
          ],
        );

    test(
      'com viagem em andamento, atualiza a lista sozinho e percebe a conclusão',
      () async {
        rides = FakeRideRepository(ride: fakeRide(status: 'IN_PROGRESS'));
        container = containerFor(rides);
        container.listen(rideCompletionWatcherProvider, (_, _) {});
        container.listen(myRidesProvider, (_, _) {});

        final first = await container.read(myRidesProvider.future);
        expect(first.single.status, RideStatus.inProgress);

        // O motorista conclui em outro aparelho: o app só fica sabendo se consultar de novo
        rides.ride = fakeRide(
          status: 'COMPLETED',
          passengerStatus: 'COMPLETED',
        );
        await Future<void>.delayed(const Duration(milliseconds: 400));

        expect(
          container.read(myRidesProvider).value!.single.status,
          RideStatus.completed,
        );
      },
    );

    test('sem mudança nas viagens, consulta mas não recarrega a lista (a tela não pisca)', () async {
      rides = FakeRideRepository(ride: fakeRide(status: 'IN_PROGRESS'));
      container = containerFor(rides);
      final events = <AsyncValue<List<RideModel>>>[];
      container.listen(rideCompletionWatcherProvider, (_, _) {});
      container.listen(myRidesProvider, (_, next) => events.add(next));

      await container.read(myRidesProvider.future);
      final eventsAfterFirstLoad = events.length;
      await Future<void>.delayed(const Duration(milliseconds: 300));

      // Consultou várias vezes, mas a lista do app não foi recarregada
      expect(
        rides.calls.where((call) => call == 'listMyRides').length,
        greaterThan(1),
      );
      expect(events, hasLength(eventsAfterFirstLoad));
    });

    test('sem viagem a caminho, não consulta de novo', () async {
      rides = FakeRideRepository(
        ride: fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED'),
      );
      container = containerFor(rides);
      container.listen(rideCompletionWatcherProvider, (_, _) {});
      container.listen(myRidesProvider, (_, _) {});

      await container.read(myRidesProvider.future);
      await Future<void>.delayed(const Duration(milliseconds: 300));

      expect(rides.calls.where((call) => call == 'listMyRides'), hasLength(1));
    });
  });
}
