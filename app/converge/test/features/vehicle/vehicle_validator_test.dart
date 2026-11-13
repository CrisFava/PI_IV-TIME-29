// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/vehicle/data/models/energy_source.dart';
import 'package:converge/features/vehicle/data/models/vehicle_type.dart';
import 'package:converge/features/vehicle/data/validation/invalid_vehicle_exception.dart';
import 'package:converge/features/vehicle/data/validation/vehicle_validator.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/valid_vehicle_request.dart';

void main() {
  group('RENAVAM', () {
    test('aceita um número com dígito verificador correto', () {
      expect(VehicleValidator.validateRenavam(validRenavam), isNull);
    });

    test('recusa dígito verificador errado, tamanho errado e repetidos', () {
      expect(VehicleValidator.validateRenavam('14283256657'), isNotNull);
      expect(VehicleValidator.validateRenavam('1428325665'), isNotNull);
      expect(VehicleValidator.validateRenavam('11111111111'), isNotNull);
      expect(VehicleValidator.validateRenavam(''), isNotNull);
    });
  });

  test('placa: aceita o formato antigo e o Mercosul', () {
    expect(VehicleValidator.validatePlate('ABC1234'), isNull);
    expect(VehicleValidator.validatePlate('ABC-1234'), isNull);
    expect(VehicleValidator.validatePlate('ABC1D23'), isNull);
    expect(VehicleValidator.validatePlate('AB12345'), isNotNull);
    expect(VehicleValidator.validatePlate('ABC12'), isNotNull);
  });

  test('capacidade: moto leva no máximo 2 lugares', () {
    expect(
      VehicleValidator.validateCapacity(2, VehicleType.motorcycle),
      isNull,
    );
    expect(
      VehicleValidator.validateCapacity(3, VehicleType.motorcycle),
      isNotNull,
    );
    expect(VehicleValidator.validateCapacity(16, VehicleType.van), isNotNull);
    expect(VehicleValidator.validateCapacity(1, VehicleType.hatch), isNotNull);
  });

  test('consumo e preço seguem as faixas do backend', () {
    expect(
      VehicleValidator.validateConsumption(
        12,
        VehicleType.hatch,
        EnergySource.gasoline,
      ),
      isNull,
    );
    expect(
      VehicleValidator.validateConsumption(
        30,
        VehicleType.hatch,
        EnergySource.gasoline,
      ),
      isNotNull,
    );
    expect(VehicleValidator.validatePrice(5.89, EnergySource.gasoline), isNull);
    expect(
      VehicleValidator.validatePrice(2.5, EnergySource.gasoline),
      isNotNull,
    );
    expect(VehicleValidator.validatePrice(1.2, EnergySource.electric), isNull);
  });

  test('validate lança InvalidVehicleException apontando o campo', () {
    expect(
      () => VehicleValidator.validate(validVehicleRequest()),
      returnsNormally,
    );

    expect(
      () => VehicleValidator.validate(
        validVehicleRequest(renavam: '12345678901'),
      ),
      throwsA(
        isA<InvalidVehicleException>().having(
          (e) => e.field,
          'field',
          'renavam',
        ),
      ),
    );
    expect(
      () => VehicleValidator.validate(
        validVehicleRequest(type: VehicleType.motorcycle, totalCapacity: 4),
      ),
      throwsA(
        isA<InvalidVehicleException>().having(
          (e) => e.field,
          'field',
          'totalCapacity',
        ),
      ),
    );
  });

  test('o pedido envia o RENAVAM só com dígitos e a placa normalizada', () {
    final json = validVehicleRequest(
      licensePlate: 'abc-1234',
      renavam: '142.832.566-56',
    ).toJson();

    expect(json['licensePlate'], 'ABC1234');
    expect(json['renavam'], validRenavam);
    expect(json['fuelSources'], [
      {
        'energySource': 'GASOLINE',
        'averageConsumption': 12.0,
        'consumptionUnit': 'KM_PER_LITER',
        'currentEnergyPrice': 5.89,
      },
    ]);
  });
}
