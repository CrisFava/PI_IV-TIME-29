// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

abstract final class VehicleEndpoints {
  static const String vehicles = '/vehicles';

  static String vehicleById(String vehicleId) => '$vehicles/$vehicleId';
}
