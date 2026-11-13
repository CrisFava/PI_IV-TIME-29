// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/vehicle/data/models/create_vehicle_request.dart';
import 'package:converge/features/vehicle/data/models/vehicle_model.dart';
import 'package:converge/features/vehicle/data/repository/vehicle_repository.dart';
import 'package:converge/features/vehicle/data/validation/vehicle_validator.dart';

/// Repositório de veículos em memória, com a mesma validação do real.
class FakeVehicleRepository implements VehicleRepository {
  final List<VehicleModel> vehicles;
  final List<String> calls = [];

  /// Faz a listagem falhar, como numa queda de rede.
  bool failList = false;

  FakeVehicleRepository([List<VehicleModel>? initial])
    : vehicles = [...?initial];

  @override
  Future<List<VehicleModel>> listMyVehicles() async {
    calls.add('list');
    if (failList) throw Exception('falha ao listar');
    return [...vehicles];
  }

  @override
  Future<VehicleModel> createVehicle(CreateVehicleRequest request) async {
    VehicleValidator.validate(request);
    calls.add('create');
    final created = VehicleModel(
      id: 'vehicle-${vehicles.length + 1}',
      model: request.model,
      licensePlate: request.licensePlate,
      color: request.color,
      type: request.type,
      totalCapacity: request.totalCapacity,
      nickname: request.nickname,
      renavam: request.renavam,
      fuelSources: [request.fuelSource, ...request.otherFuelSources],
    );
    vehicles.add(created);
    return created;
  }

  @override
  Future<VehicleModel> updateVehicle(
    String vehicleId,
    CreateVehicleRequest request,
  ) async {
    VehicleValidator.validate(request);
    calls.add('update:$vehicleId');
    final updated = VehicleModel(
      id: vehicleId,
      model: request.model,
      licensePlate: request.licensePlate,
      color: request.color,
      type: request.type,
      totalCapacity: request.totalCapacity,
      nickname: request.nickname,
      renavam: request.renavam,
      fuelSources: [request.fuelSource, ...request.otherFuelSources],
    );
    final index = vehicles.indexWhere((vehicle) => vehicle.id == vehicleId);
    if (index >= 0) vehicles[index] = updated;
    return updated;
  }

  @override
  Future<void> deleteVehicle(String vehicleId) async {
    calls.add('delete:$vehicleId');
    vehicles.removeWhere((vehicle) => vehicle.id == vehicleId);
  }
}
