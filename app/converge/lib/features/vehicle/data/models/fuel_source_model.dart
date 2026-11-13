// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import 'energy_source.dart';

/// Combustível do veículo: consumo médio e preço atual da energia.
@immutable
class FuelSourceModel {
  final EnergySource energySource;
  final double averageConsumption;
  final double currentEnergyPrice;

  const FuelSourceModel({
    required this.energySource,
    required this.averageConsumption,
    required this.currentEnergyPrice,
  });

  factory FuelSourceModel.fromJson(
    Map<String, dynamic> json,
  ) => FuelSourceModel(
    energySource: EnergySource.fromApi(json['energySource'] as String?),
    averageConsumption: (json['averageConsumption'] as num?)?.toDouble() ?? 0,
    currentEnergyPrice: (json['currentEnergyPrice'] as num?)?.toDouble() ?? 0,
  );

  Map<String, dynamic> toJson() => {
    'energySource': energySource.apiValue,
    'averageConsumption': averageConsumption,
    'consumptionUnit': energySource.consumptionUnitApi,
    'currentEnergyPrice': currentEnergyPrice,
  };
}
