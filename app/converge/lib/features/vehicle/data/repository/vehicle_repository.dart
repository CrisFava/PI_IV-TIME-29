// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import '../models/vehicle_summary_model.dart';

/// Contrato com a API de veículos (`/vehicles`).
abstract class VehicleRepository {
  /// Veículos do usuário logado.
  Future<List<VehicleSummaryModel>> listMyVehicles();
}
