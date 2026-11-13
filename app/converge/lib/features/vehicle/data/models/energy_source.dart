// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Fonte de energia do veículo, espelhando o enum `EnergySource` do backend.
enum EnergySource {
  gasoline('GASOLINE', 'Gasolina'),
  ethanol('ETHANOL', 'Etanol'),
  diesel('DIESEL', 'Diesel'),
  electric('ELECTRIC', 'Elétrico');

  final String apiValue;
  final String label;

  const EnergySource(this.apiValue, this.label);

  bool get isElectric => this == EnergySource.electric;

  /// Unidade de consumo enviada ao backend (`ConsumptionUnit`).
  String get consumptionUnitApi => isElectric ? 'KM_PER_KWH' : 'KM_PER_LITER';

  /// Unidade de consumo para exibir (km/l ou km/kWh).
  String get consumptionUnitLabel => isElectric ? 'km/kWh' : 'km/l';

  /// Unidade do preço para exibir (R$/l ou R$/kWh).
  String get priceUnitLabel => isElectric ? 'R\$/kWh' : 'R\$/l';

  static EnergySource fromApi(String? value) => EnergySource.values.firstWhere(
    (source) => source.apiValue == value,
    orElse: () => EnergySource.gasoline,
  );
}
