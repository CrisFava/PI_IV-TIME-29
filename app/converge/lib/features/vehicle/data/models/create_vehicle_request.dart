// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import 'energy_source.dart';
import 'fuel_source_model.dart';
import 'vehicle_model.dart';
import 'vehicle_type.dart';

/// Corpo do `POST /vehicles` (cadastro) e do `PUT /vehicles/{id}` (edição).
/// O backend também valida o RENAVAM (dígito verificador).
@immutable
class CreateVehicleRequest {
  final String? nickname;
  final String model;
  final String licensePlate;
  final String color;
  final VehicleType type;
  final int totalCapacity;
  final String renavam;
  final FuelSourceModel fuelSource;

  /// Demais fontes de energia do veículo (ex.: híbrido). O formulário só edita a
  /// principal; estas são mantidas para a edição não apagá-las.
  final List<FuelSourceModel> otherFuelSources;

  const CreateVehicleRequest({
    required this.model,
    required this.licensePlate,
    required this.color,
    required this.type,
    required this.totalCapacity,
    required this.renavam,
    required this.fuelSource,
    this.nickname,
    this.otherFuelSources = const [],
  });

  /// Preenche o formulário de edição com os dados atuais do veículo.
  factory CreateVehicleRequest.fromVehicle(VehicleModel vehicle) =>
      CreateVehicleRequest(
        nickname: vehicle.nickname,
        model: vehicle.model,
        licensePlate: vehicle.licensePlate,
        color: vehicle.color,
        type: vehicle.type,
        totalCapacity: vehicle.totalCapacity,
        renavam: vehicle.renavam ?? '',
        fuelSource: vehicle.fuelSources.isNotEmpty
            ? vehicle.fuelSources.first
            : const FuelSourceModel(
                energySource: EnergySource.gasoline,
                averageConsumption: 0,
                currentEnergyPrice: 0,
              ),
        otherFuelSources: vehicle.fuelSources.length > 1
            ? List.unmodifiable(vehicle.fuelSources.skip(1))
            : const [],
      );

  Map<String, dynamic> toJson() => {
    if (nickname?.trim().isNotEmpty ?? false) 'nickname': nickname!.trim(),
    'model': model.trim(),
    'licensePlate': licensePlate.replaceAll('-', '').trim().toUpperCase(),
    'color': color.trim(),
    'type': type.apiValue,
    'totalCapacity': totalCapacity,
    'renavam': renavam.replaceAll(RegExp(r'\D'), ''),
    'fuelSources': [
      fuelSource.toJson(),
      for (final other in otherFuelSources) other.toJson(),
    ],
  };
}
