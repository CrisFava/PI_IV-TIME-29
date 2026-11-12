// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_participant_model.dart';

/// Um passageiro numa viagem do motorista logado: um pedido de vaga
/// esperando resposta ou um passageiro já aceito (com o chat liberado).
@immutable
class DriverRideRequest {
  final RideModel ride;
  final RideParticipantModel passenger;

  const DriverRideRequest({required this.ride, required this.passenger});

  String get rideId => ride.id;
  String get passengerId => passenger.userId ?? '';

  /// Chave única do pedido, usada para marcar o que está em andamento.
  String get key => '${ride.id}:$passengerId';
}
