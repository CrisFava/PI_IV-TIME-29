// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/auth/data/models/participation_role.dart';
import 'package:converge/features/review/data/models/review_model.dart';
import 'package:converge/features/review/state/pending_reviews.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';

/// Passageiro extra da viagem, no formato que o backend devolve.
Map<String, dynamic> passengerJson(String id, String name, String status) => {
  'userId': id,
  'name': name,
  'role': 'PASSENGER',
  'status': status,
  'paymentStatus': 'PENDING',
  'paymentConfirmedByDriver': false,
};

/// Avaliação que `reviewerId` já fez de `reviewedId`.
ReviewModel givenTo(String reviewedId) => ReviewModel(
  id: 'review-$reviewedId',
  rideId: fakeRideId,
  reviewerId: 'quem-avaliou',
  reviewedId: reviewedId,
  reviewedRole: ParticipationRole.passenger,
  rating: 5,
  comment: null,
  createdAt: null,
);

void main() {
  // fakeRide: motorista 'driver-1' (Lucas Silveira) e passageira 'passenger-1' (Ana Souza)
  final completedRide = fakeRide(
    status: 'COMPLETED',
    passengerStatus: 'COMPLETED',
  );

  group('passageiro', () {
    test('avalia o motorista quando a viagem foi concluída', () {
      final targets = pendingReviewTargets(
        ride: completedRide,
        viewerId: fakePassengerId,
        given: const [],
      );

      expect(targets, hasLength(1));
      expect(targets.single.userId, fakeDriverId);
      expect(targets.single.name, 'Lucas Silveira');
      expect(targets.single.isDriver, isTrue);
    });

    test(
      'avalia o motorista assim que desce, com a viagem ainda em andamento',
      () {
        final targets = pendingReviewTargets(
          ride: fakeRide(status: 'IN_PROGRESS', passengerStatus: 'COMPLETED'),
          viewerId: fakePassengerId,
          given: const [],
        );

        expect(targets.single.userId, fakeDriverId);
      },
    );

    test('não tem mais ninguém para avaliar depois de avaliar o motorista', () {
      final targets = pendingReviewTargets(
        ride: completedRide,
        viewerId: fakePassengerId,
        given: [givenTo(fakeDriverId)],
      );

      expect(targets, isEmpty);
    });

    test('quem faltou à viagem não avalia', () {
      final ride = fakeRide(status: 'COMPLETED', passengerStatus: 'NO_SHOW');

      final targets = pendingReviewTargets(
        ride: ride,
        viewerId: fakePassengerId,
        given: const [],
      );

      expect(targets, isEmpty);
    });
  });

  group('motorista', () {
    test('avalia cada passageiro que viajou, só os que ainda não avaliou', () {
      final ride = fakeRide(
        status: 'COMPLETED',
        passengerStatus: 'COMPLETED',
        extraParticipants: [
          passengerJson('passenger-2', 'Bia Lima', 'COMPLETED'),
        ],
      );

      final all = pendingReviewTargets(
        ride: ride,
        viewerId: fakeDriverId,
        given: const [],
      );
      final afterAna = pendingReviewTargets(
        ride: ride,
        viewerId: fakeDriverId,
        given: [givenTo(fakePassengerId)],
      );

      expect(all.map((target) => target.name), ['Ana Souza', 'Bia Lima']);
      expect(all.every((target) => !target.isDriver), isTrue);
      expect(afterAna.map((target) => target.userId), ['passenger-2']);
    });

    test('não avalia quem faltou nem quem cancelou', () {
      final ride = fakeRide(
        status: 'COMPLETED',
        passengerStatus: 'COMPLETED',
        extraParticipants: [
          passengerJson('passenger-2', 'Bia Lima', 'NO_SHOW'),
          passengerJson('passenger-3', 'Caio Melo', 'CANCELLED'),
        ],
      );

      final targets = pendingReviewTargets(
        ride: ride,
        viewerId: fakeDriverId,
        given: const [],
      );

      expect(targets.map((target) => target.userId), [fakePassengerId]);
    });
  });

  group('viagem sem avaliação possível', () {
    test('viagem que ainda não terminou não tem ninguém para avaliar', () {
      for (final status in ['CONFIRMED', 'IN_PROGRESS', 'CANCELLED']) {
        final ride = fakeRide(status: status);

        expect(
          pendingReviewTargets(
            ride: ride,
            viewerId: fakePassengerId,
            given: const [],
          ),
          isEmpty,
          reason: 'passageiro, viagem $status',
        );
        expect(
          pendingReviewTargets(
            ride: ride,
            viewerId: fakeDriverId,
            given: const [],
          ),
          isEmpty,
          reason: 'motorista, viagem $status',
        );
      }
    });

    test('quem não participou da viagem não avalia ninguém', () {
      final targets = pendingReviewTargets(
        ride: completedRide,
        viewerId: 'desconhecido',
        given: const [],
      );

      expect(targets, isEmpty);
    });
  });

  group('viagem concluída há pouco (lembrete ao abrir o app)', () {
    // fakeRide concluída termina em 16/11/2026 às 08:12
    test('dentro das últimas 24 horas aparece', () {
      final rides = recentlyCompletedRides(
        rides: [completedRide],
        now: DateTime(2026, 11, 16, 20),
      );

      expect(rides.map((ride) => ride.id), [fakeRideId]);
    });

    test('concluída há mais de 24 horas não aparece', () {
      final rides = recentlyCompletedRides(
        rides: [completedRide],
        now: DateTime(2026, 11, 18),
      );

      expect(rides, isEmpty);
    });

    test('viagem que não está concluída não aparece', () {
      final rides = recentlyCompletedRides(
        rides: [
          fakeRide(status: 'IN_PROGRESS'),
          fakeRide(status: 'CANCELLED'),
        ],
        now: DateTime(2026, 11, 16, 20),
      );

      expect(rides, isEmpty);
    });
  });

  group('viagem que acabou de ser concluída', () {
    final inProgress = fakeRide(status: 'IN_PROGRESS');

    test('aparece quando passa de em andamento para concluída', () {
      final finished = newlyCompletedRides(
        previous: [inProgress],
        next: [completedRide],
      );

      expect(finished.map((ride) => ride.id), [fakeRideId]);
    });

    test('na primeira carga não há nenhuma, mesmo com viagens concluídas', () {
      final finished = newlyCompletedRides(
        previous: null,
        next: [completedRide],
      );

      expect(finished, isEmpty);
    });

    test('viagem que já estava concluída não aparece de novo', () {
      final finished = newlyCompletedRides(
        previous: [completedRide],
        next: [completedRide],
      );

      expect(finished, isEmpty);
    });

    test('viagem que ainda não existia na lista anterior não conta', () {
      final finished = newlyCompletedRides(
        previous: const [],
        next: [completedRide],
      );

      expect(finished, isEmpty);
    });

    test('viagem cancelada ou ainda em andamento não conta', () {
      final finished = newlyCompletedRides(
        previous: [fakeRide(status: 'CONFIRMED')],
        next: [fakeRide(status: 'CANCELLED')],
      );

      expect(finished, isEmpty);
    });
  });
}
