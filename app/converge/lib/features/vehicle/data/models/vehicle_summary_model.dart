// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

/// Resumo de um veículo do usuário, conforme `VehicleResponseDTO`.
/// Só o que o app precisa para montar um trajeto de oferta.
@immutable
class VehicleSummaryModel {
  final String id;
  final String model;
  final int totalCapacity;
  final bool active;

  const VehicleSummaryModel({
    required this.id,
    required this.model,
    required this.totalCapacity,
    required this.active,
  });

  /// Vagas para passageiros: a capacidade menos o motorista.
  int get passengerSeats => totalCapacity > 1 ? totalCapacity - 1 : 1;

  factory VehicleSummaryModel.fromJson(Map<String, dynamic> json) =>
      VehicleSummaryModel(
        id: json['id'] as String,
        model: json['model'] as String? ?? 'Veículo',
        totalCapacity: (json['totalCapacity'] as num?)?.toInt() ?? 2,
        active: json['active'] as bool? ?? true,
      );
}
