// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../../route/data/models/route_place_model.dart';
import 'ride_stop_type.dart';

/// Uma parada do itinerário da viagem, conforme `RideStopResponseDTO`.
@immutable
class RideStopModel {
  final RideStopType type;
  final String passengerId;
  final RoutePlaceModel place;

  const RideStopModel({
    required this.type,
    required this.passengerId,
    required this.place,
  });

  factory RideStopModel.fromJson(Map<String, dynamic> json) => RideStopModel(
    type: RideStopType.fromJson(json['type']),
    passengerId: json['passengerId'] as String? ?? '',
    place: RoutePlaceModel.fromJson(
      (json['place'] as Map<String, dynamic>?) ?? const {},
    ),
  );
}
