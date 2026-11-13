// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/data/models/match_model.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_matches.dart';

void main() {
  test('lê a combinação do backend com o veículo e a próxima data', () {
    final match = MatchModel.fromJson(fakeMatchJson());

    expect(match.driverRouteId, 'route-1');
    expect(match.passengerRouteId, fakeSearchRouteId);
    expect(match.otherUserName, 'Lucas Silveira');
    expect(match.otherUserAverageRating, 4.8);
    expect(match.compatibilityLabel, '92%');
    expect(match.compatibleDays, {'MONDAY', 'WEDNESDAY'});
    expect(match.estimatedCostPerPassenger, 6.4);
    expect(match.maxAmount, 9.6);
    expect(match.nextRideDate, DateTime(2026, 11, 16));
    expect(match.vehicle?.description, 'Onix • Preto • ABC1D23');
    expect(match.origin.latitude, -22.9064);
    expect(match.canRequestSeat, isTrue);
  });

  test('sem próxima data, não dá para pedir a vaga', () {
    final match = fakeMatch(nextRideDate: null);

    expect(match.nextRideDate, isNull);
    expect(match.canRequestSeat, isFalse);
  });

  test('visão do motorista vem sem veículo nem vagas', () {
    final json = fakeMatchJson()
      ..remove('vehicle')
      ..remove('availableSeats')
      ..['otherUserAverageRating'] = null;

    final match = MatchModel.fromJson(json);

    expect(match.vehicle, isNull);
    expect(match.availableSeats, isNull);
    expect(match.otherUserAverageRating, isNull);
  });
}
