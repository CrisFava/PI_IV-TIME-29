// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/ride/data/models/ride_model.dart';

const fakeDriverId = 'driver-1';
const fakePassengerId = 'passenger-1';
const fakeRideId = 'ride-1';

/// JSON de uma viagem como o backend devolve para o motorista (vê todos por completo).
Map<String, dynamic> fakeRideJson({
  String status = 'CONFIRMED',
  String? chatRoomId = 'ride_ride-1',
  String passengerStatus = 'CONFIRMED',
  List<Map<String, dynamic>> extraParticipants = const [],
}) => {
  'id': fakeRideId,
  'sequentialNumber': 7,
  'driverId': fakeDriverId,
  'vehicleId': 'vehicle-1',
  'vehicleSnapshot': {
    'model': 'Onix',
    'licensePlate': 'ABC1D23',
    'type': 'HATCH',
    'color': 'Preto',
  },
  'driverRouteId': 'route-1',
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
  'departureDateTime': '2026-11-16T07:30:00',
  'estimatedArrivalDateTime': '2026-11-16T08:10:00',
  'completedAt': status == 'COMPLETED' ? '2026-11-16T08:12:00' : null,
  'weekDays': ['MONDAY'],
  'totalSeats': 3,
  'availableSeats': 2,
  'status': status,
  'chatRoomId': chatRoomId,
  'participants': [
    {
      'userId': fakeDriverId,
      'name': 'Lucas Silveira',
      'role': 'DRIVER',
      'status': status == 'COMPLETED' ? 'COMPLETED' : 'CONFIRMED',
      'paymentStatus': 'NOT_APPLICABLE',
      'paymentConfirmedByDriver': false,
    },
    {
      'userId': fakePassengerId,
      'name': 'Ana Souza',
      'role': 'PASSENGER',
      'status': passengerStatus,
      'paymentStatus': 'PENDING',
      'paymentConfirmedByDriver': false,
      'boardingLocation': {
        'address': 'Rua Coronel Quirino, 900',
        'city': 'Campinas',
        'latitude': -22.8955,
        'longitude': -47.0525,
      },
      'dropOffLocation': {
        'address': 'PUC-Campinas',
        'city': 'Campinas',
        'latitude': -22.8338,
        'longitude': -47.0519,
      },
    },
    ...extraParticipants,
  ],
};

RideModel fakeRide({
  String status = 'CONFIRMED',
  String? chatRoomId = 'ride_ride-1',
  String passengerStatus = 'CONFIRMED',
  List<Map<String, dynamic>> extraParticipants = const [],
}) => RideModel.fromJson(
  fakeRideJson(
    status: status,
    chatRoomId: chatRoomId,
    passengerStatus: passengerStatus,
    extraParticipants: extraParticipants,
  ),
);
