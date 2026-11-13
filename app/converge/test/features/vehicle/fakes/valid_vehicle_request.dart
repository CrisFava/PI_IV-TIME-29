// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/vehicle/data/models/create_vehicle_request.dart';
import 'package:converge/features/vehicle/data/models/energy_source.dart';
import 'package:converge/features/vehicle/data/models/fuel_source_model.dart';
import 'package:converge/features/vehicle/data/models/vehicle_type.dart';

/// RENAVAM com dígito verificador correto.
const validRenavam = '14283256656';

/// Um carro válido para os testes; troque só o campo que quiser invalidar.
CreateVehicleRequest validVehicleRequest({
  String model = 'Onix 1.0',
  String licensePlate = 'ABC1D23',
  String color = 'Prata',
  VehicleType type = VehicleType.hatch,
  int totalCapacity = 5,
  String renavam = validRenavam,
  EnergySource energySource = EnergySource.gasoline,
  double averageConsumption = 12,
  double currentEnergyPrice = 5.89,
}) => CreateVehicleRequest(
  model: model,
  licensePlate: licensePlate,
  color: color,
  type: type,
  totalCapacity: totalCapacity,
  renavam: renavam,
  fuelSource: FuelSourceModel(
    energySource: energySource,
    averageConsumption: averageConsumption,
    currentEnergyPrice: currentEnergyPrice,
  ),
);
