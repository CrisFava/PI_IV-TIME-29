// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/data/models/match_model.dart';

const fakeSearchRouteId = 'search-1';

/// JSON de uma combinação como o backend devolve na visão do passageiro.
Map<String, dynamic> fakeMatchJson({
  String driverRouteId = 'route-1',
  String driverName = 'Lucas Silveira',
  double score = 0.92,
  String? nextRideDate = '2026-11-16',
}) => {
  'driverRouteId': driverRouteId,
  'passengerRouteId': fakeSearchRouteId,
  'otherUserId': 'driver-$driverRouteId',
  'otherUserName': driverName,
  'otherUserAverageRating': 4.8,
  'otherUserTotalReviews': 12,
  'compatibilityScore': score,
  'calculatedDetourKm': 0.6,
  'scheduleDifferenceMin': 10,
  'compatibleDays': ['MONDAY', 'WEDNESDAY'],
  'estimatedCostPerPassenger': 6.4,
  'maxAmount': 9.6,
  'nextRideDate': nextRideDate,
  'departureTime': '07:30',
  'estimatedArrivalTime': '08:10',
  'origin': {
    'address': 'Rua das Flores, 50',
    'neighborhood': 'Centro',
    'city': 'Campinas',
    'latitude': -22.9064,
    'longitude': -47.0616,
  },
  'destination': {
    'address': 'PUC-Campinas',
    'neighborhood': 'Parque das Universidades',
    'city': 'Campinas',
    'latitude': -22.8338,
    'longitude': -47.0519,
  },
  'availableSeats': 2,
  'vehicle': {'model': 'Onix', 'color': 'Preto', 'licensePlate': 'ABC1D23'},
};

MatchModel fakeMatch({
  String driverRouteId = 'route-1',
  String driverName = 'Lucas Silveira',
  double score = 0.92,
  String? nextRideDate = '2026-11-16',
}) => MatchModel.fromJson(
  fakeMatchJson(
    driverRouteId: driverRouteId,
    driverName: driverName,
    score: score,
    nextRideDate: nextRideDate,
  ),
);
