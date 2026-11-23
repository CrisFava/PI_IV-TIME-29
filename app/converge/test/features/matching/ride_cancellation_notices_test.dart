// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/state/ride_cancellation_notices.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';

void main() {
  test('cancelamento avisa o outro lado, e não quem cancelou', () {
    final confirmed = fakeRide();
    final passengerLeft = fakeRide(passengerStatus: 'CANCELLED');
    final cancelledByDriver = RideModel.fromJson({
      ...fakeRideJson(status: 'CANCELLED'),
      'cancelledBy': fakeDriverId,
    });

    // O passageiro saiu: o motorista é avisado; o passageiro, não
    expect(
      rideCancellationNotices(
        before: [confirmed],
        after: [passengerLeft],
        viewerId: fakeDriverId,
      ).single.forDriver,
      isTrue,
    );
    expect(
      rideCancellationNotices(
        before: [confirmed],
        after: [passengerLeft],
        viewerId: fakePassengerId,
      ),
      isEmpty,
    );

    // O motorista cancelou a carona: o passageiro é avisado; o motorista, não
    expect(
      rideCancellationNotices(
        before: [confirmed],
        after: [cancelledByDriver],
        viewerId: fakePassengerId,
      ).single.title,
      'Carona cancelada',
    );
    expect(
      rideCancellationNotices(
        before: [confirmed],
        after: [cancelledByDriver],
        viewerId: fakeDriverId,
      ),
      isEmpty,
    );
  });
}
