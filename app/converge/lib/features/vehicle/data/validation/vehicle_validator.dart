// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import '../models/create_vehicle_request.dart';
import '../models/energy_source.dart';
import '../models/vehicle_type.dart';
import 'invalid_vehicle_exception.dart';

/// Regras de um veículo válido. São as mesmas do backend (`Vehicle` e
/// `CreateVehicleRequestDTO`), mais o RENAVAM, para o app recusar os dados antes
/// de enviá-los. Cada `validateX` devolve a mensagem de erro ou null.
abstract final class VehicleValidator {
  // ABC1234 (antiga) ou ABC1D23 (Mercosul), com hífen opcional
  static final _platePattern = RegExp(r'^[A-Za-z]{3}-?\d[A-Za-z0-9]\d{2}$');

  // Pesos do dígito verificador do RENAVAM, para os 10 primeiros dígitos
  static const _renavamWeights = [3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  static String? validateModel(String? value) {
    final model = value?.trim() ?? '';
    if (model.isEmpty) return 'Informe o modelo do veículo.';
    if (model.length < 2) return 'O modelo deve ter pelo menos 2 letras.';
    return null;
  }

  static String? validateColor(String? value) {
    if ((value?.trim() ?? '').isEmpty) return 'Informe a cor do veículo.';
    return null;
  }

  static String? validatePlate(String? value) {
    final plate = value?.trim() ?? '';
    if (plate.isEmpty) return 'Informe a placa do veículo.';
    if (!_platePattern.hasMatch(plate)) {
      return 'A placa deve estar no formato ABC1234 ou ABC1D23.';
    }
    return null;
  }

  /// RENAVAM: 11 dígitos, sendo o último o dígito verificador.
  static String? validateRenavam(String? value) {
    final digits = (value ?? '').replaceAll(RegExp(r'\D'), '');
    if (digits.isEmpty) return 'Informe o RENAVAM do veículo.';
    if (digits.length != 11) return 'O RENAVAM deve ter 11 dígitos.';
    if (RegExp(r'^(\d)\1{10}$').hasMatch(digits)) {
      return 'RENAVAM inválido.';
    }

    var sum = 0;
    for (var i = 0; i < 10; i++) {
      sum += int.parse(digits[i]) * _renavamWeights[i];
    }
    final checkDigit = (sum * 10) % 11 % 10;
    if (checkDigit != int.parse(digits[10])) return 'RENAVAM inválido.';
    return null;
  }

  static String? validateCapacity(int capacity, VehicleType type) {
    if (capacity < 1 || capacity > 15) {
      return 'A capacidade deve ficar entre 1 e 15 lugares.';
    }
    if (type.isMotorcycle && capacity > 2) {
      return 'Uma moto comporta no máximo 2 lugares (piloto e 1 passageiro).';
    }
    if (capacity < 2) {
      return 'O veículo precisa de pelo menos 1 lugar para passageiro.';
    }
    return null;
  }

  /// Faixa de consumo aceita pelo backend para o tipo e a energia.
  static (double, double) consumptionRange(
    VehicleType type,
    EnergySource source,
  ) {
    final electric = source.isElectric;
    return switch (type) {
      VehicleType.hatch || VehicleType.sedan => electric ? (3, 9) : (6, 25),
      VehicleType.suv ||
      VehicleType.pickup ||
      VehicleType.van => electric ? (3, 9) : (4, 18),
      VehicleType.motorcycle => electric ? (15, 40) : (15, 50),
      VehicleType.other => electric ? (3, 40) : (4, 50),
    };
  }

  /// Faixa de preço aceita pelo backend para a energia.
  static (double, double) priceRange(EnergySource source) =>
      source.isElectric ? (0.30, 3.00) : (3.00, 12.00);

  static String? validateConsumption(
    double? value,
    VehicleType type,
    EnergySource source,
  ) {
    if (value == null) return 'Informe o consumo médio.';
    final (min, max) = consumptionRange(type, source);
    if (value < min || value > max) {
      return 'Para ${type.label.toLowerCase()} a ${source.label.toLowerCase()}, '
          'o consumo deve ficar entre ${_number(min)} e ${_number(max)} '
          '${source.consumptionUnitLabel}.';
    }
    return null;
  }

  static String? validatePrice(double? value, EnergySource source) {
    if (value == null) return 'Informe o preço da energia.';
    final (min, max) = priceRange(source);
    if (value < min || value > max) {
      return 'O preço de ${source.label.toLowerCase()} deve ficar entre '
          'R\$ ${_money(min)} e R\$ ${_money(max)}.';
    }
    return null;
  }

  /// Valida o pedido inteiro e lança [InvalidVehicleException] no primeiro erro.
  static void validate(CreateVehicleRequest request) {
    final fuel = request.fuelSource;
    final checks = <String, String?>{
      'model': validateModel(request.model),
      'licensePlate': validatePlate(request.licensePlate),
      'color': validateColor(request.color),
      'renavam': validateRenavam(request.renavam),
      'totalCapacity': validateCapacity(request.totalCapacity, request.type),
      'averageConsumption': validateConsumption(
        fuel.averageConsumption,
        request.type,
        fuel.energySource,
      ),
      'currentEnergyPrice': validatePrice(
        fuel.currentEnergyPrice,
        fuel.energySource,
      ),
    };

    for (final MapEntry(key: field, value: error) in checks.entries) {
      if (error != null) throw InvalidVehicleException(error, field: field);
    }
  }

  /// Lê números digitados com vírgula ou ponto ("12,5" ou "12.5").
  static double? parseDecimal(String? value) =>
      double.tryParse((value ?? '').trim().replaceAll(',', '.'));

  static String _number(double value) =>
      value == value.roundToDouble() ? value.toStringAsFixed(0) : '$value';

  static String _money(double value) =>
      value.toStringAsFixed(2).replaceAll('.', ',');
}
