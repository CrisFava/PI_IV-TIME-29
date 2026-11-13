// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Tipo do veículo, espelhando o enum `VehicleType` do backend.
enum VehicleType {
  hatch('HATCH', 'Hatch'),
  sedan('SEDAN', 'Sedã'),
  suv('SUV', 'SUV'),
  pickup('PICKUP', 'Picape'),
  van('VAN', 'Van'),
  motorcycle('MOTORCYCLE', 'Moto'),
  other('OTHER', 'Outro');

  final String apiValue;
  final String label;

  const VehicleType(this.apiValue, this.label);

  bool get isMotorcycle => this == VehicleType.motorcycle;

  /// Lugares no veículo, contando o motorista (regra do backend).
  int get maxCapacity => isMotorcycle ? 2 : 15;

  static VehicleType fromApi(String? value) => VehicleType.values.firstWhere(
    (type) => type.apiValue == value,
    orElse: () => VehicleType.other,
  );
}
