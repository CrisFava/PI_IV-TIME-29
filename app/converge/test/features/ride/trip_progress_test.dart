// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/models/ride_stop_type.dart';
import 'package:converge/features/ride/state/trip_progress.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_ride.dart';

Map<String, dynamic> _place(double lat, double lng) => {
  'address': 'Ponto $lat',
  'city': 'Campinas',
  'latitude': lat,
  'longitude': lng,
};

Map<String, dynamic> _stop(String type, String passengerId, double lat) => {
  'type': type,
  'passengerId': passengerId,
  'place': _place(lat, -47.05),
};

/// Ana e Bruno embarcam em pontos diferentes e descem juntos na PUC.
RideModel _ride({
  String? anaBoardedAt,
  String anaStatus = 'CONFIRMED',
  String brunoStatus = 'CONFIRMED',
}) {
  final json = fakeRideJson(
    status: 'IN_PROGRESS',
    passengerStatus: anaStatus,
    extraParticipants: [
      {
        'userId': 'passenger-2',
        'name': 'Bruno Lima',
        'role': 'PASSENGER',
        'status': brunoStatus,
      },
    ],
  );
  if (anaBoardedAt != null) {
    (json['participants'] as List)[1]['boardedAt'] = anaBoardedAt;
  }
  json['itinerary'] = [
    _stop('PICKUP', fakePassengerId, -22.90),
    _stop('PICKUP', 'passenger-2', -22.88),
    _stop('DROPOFF', fakePassengerId, -22.834),
    _stop('DROPOFF', 'passenger-2', -22.834),
  ];
  return RideModel.fromJson(json);
}

void main() {
  test('a próxima etapa avança quando o motorista confirma quem entrou', () {
    final before = TripProgress.of(_ride());
    expect(before.current!.passengers.single.displayName, 'Ana Souza');
    // 5 trechos (4 paradas + destino): o atual vai até a Ana, o resto vem depois
    final legs = before.legsFor(5);
    expect(legs.current, [0]);
    expect(legs.upcoming, [1, 2, 3, 4]);

    final after = TripProgress.of(_ride(anaBoardedAt: '2026-11-16T07:35:00'));
    expect(after.current!.passengers.single.displayName, 'Bruno Lima');
    expect(after.legsFor(5).current, [1]);
  });

  test('quem desce no mesmo ponto forma uma etapa só', () {
    final progress = TripProgress.of(_ride());

    expect(progress.steps, hasLength(3));
    final dropOff = progress.steps.last;
    expect(dropOff.type, RideStopType.dropOff);
    expect(dropOff.passengers, hasLength(2));
    // A etapa cobre as duas paradas: os trechos entre elas são parte do mesmo trecho
    expect((dropOff.firstStop, dropOff.lastStop), (2, 3));
  });

  test('sem paradas pendentes, o trecho atual é o último, até o destino', () {
    final progress = TripProgress.of(
      _ride(anaStatus: 'COMPLETED', brunoStatus: 'COMPLETED'),
    );

    expect(progress.finished, isTrue);
    expect(progress.legsFor(5).current, [4]);
    expect(progress.legsFor(5).upcoming, isEmpty);
  });
}
