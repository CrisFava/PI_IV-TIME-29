// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

/// Veículo do motorista de uma combinação, conforme `MatchVehicleDTO`.
@immutable
class MatchVehicleModel {
  final String model;
  final String? color;
  final String? licensePlate;

  const MatchVehicleModel({required this.model, this.color, this.licensePlate});

  /// Ex.: `Volkswagen Gol • Azul • ABC1234`.
  String get description => [
    model,
    if (color != null && color!.isNotEmpty) color!,
    if (licensePlate != null && licensePlate!.isNotEmpty) licensePlate!,
  ].join(' • ');

  factory MatchVehicleModel.fromJson(Map<String, dynamic> json) =>
      MatchVehicleModel(
        model: json['model'] as String? ?? 'Veículo',
        color: json['color'] as String?,
        licensePlate: json['licensePlate'] as String?,
      );
}
