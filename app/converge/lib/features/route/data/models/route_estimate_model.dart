// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

/// Prévia do caminho entre a partida e o destino, antes de publicar ou pedir a
/// carona: distância, tempo de carro (pela Routes API do Google) e o desenho.
@immutable
class RouteEstimateModel {
  final double distanceKm;
  final int? durationMinutes;
  final String? encodedPolyline;

  const RouteEstimateModel({
    required this.distanceKm,
    this.durationMinutes,
    this.encodedPolyline,
  });

  factory RouteEstimateModel.fromJson(Map<String, dynamic> json) =>
      RouteEstimateModel(
        distanceKm: (json['distanceKm'] as num?)?.toDouble() ?? 0,
        durationMinutes: (json['durationMinutes'] as num?)?.toInt(),
        encodedPolyline: json['encodedPolyline'] as String?,
      );
}
