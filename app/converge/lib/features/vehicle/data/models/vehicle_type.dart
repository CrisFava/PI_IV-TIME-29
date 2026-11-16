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

  /// Van ou outro (micro-ônibus etc.): os únicos que passam de 7 lugares.
  bool get isLarge => this == VehicleType.van || this == VehicleType.other;

  /// Lugares no veículo, contando o motorista (regra do backend):
  /// moto até 2, carro até 7, van ou outro até 15.
  int get maxCapacity => isMotorcycle ? 2 : (isLarge ? 15 : 7);

  static VehicleType fromApi(String? value) => VehicleType.values.firstWhere(
    (type) => type.apiValue == value,
    orElse: () => VehicleType.other,
  );
}
