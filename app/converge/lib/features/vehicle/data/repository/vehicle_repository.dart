// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import '../models/create_vehicle_request.dart';
import '../models/vehicle_model.dart';

/// Contrato com a API de veículos (`/vehicles`).
abstract class VehicleRepository {
  /// Veículos do usuário logado.
  Future<List<VehicleModel>> listMyVehicles();

  /// Cadastra um veículo. Valida os dados antes e lança `InvalidVehicleException`
  /// se algo estiver errado, sem chamar a API.
  Future<VehicleModel> createVehicle(CreateVehicleRequest request);

  /// Edita um veículo. Também valida antes e lança `InvalidVehicleException`.
  Future<VehicleModel> updateVehicle(
    String vehicleId,
    CreateVehicleRequest request,
  );

  /// Remove (desativa) um veículo do usuário.
  Future<void> deleteVehicle(String vehicleId);
}
