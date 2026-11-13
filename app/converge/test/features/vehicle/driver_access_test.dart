// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/auth/data/models/participation_role.dart';
import 'package:converge/features/auth/data/models/user_model.dart';
import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:converge/features/vehicle/data/models/create_vehicle_request.dart';
import 'package:converge/features/vehicle/data/models/vehicle_model.dart';
import 'package:converge/features/vehicle/data/models/vehicle_type.dart';
import 'package:converge/features/vehicle/data/repository/vehicle_repository_impl.dart';
import 'package:converge/features/vehicle/data/validation/invalid_vehicle_exception.dart';
import 'package:converge/features/vehicle/state/driver_access.dart';
import 'package:converge/features/vehicle/state/driver_access_provider.dart';
import 'package:converge/features/vehicle/state/my_vehicles_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../auth/fakes/fake_user.dart';
import 'fakes/fake_signed_in_auth_controller.dart';
import 'fakes/fake_vehicle_repository.dart';
import 'fakes/valid_vehicle_request.dart';

void main() {
  const onix = VehicleModel(
    id: 'vehicle-1',
    model: 'Onix',
    licensePlate: 'ABC1D23',
    color: 'Prata',
    type: VehicleType.hatch,
    totalCapacity: 5,
  );

  UserModel driver() => fakeUser().copyWith(
    participationRoles: {ParticipationRole.passenger, ParticipationRole.driver},
  );

  FakeSignedInAuthController? auth;

  ProviderContainer containerFor(
    UserModel user,
    FakeVehicleRepository vehicles,
  ) {
    final container = ProviderContainer.test(
      overrides: [
        authControllerProvider.overrideWith(
          () => auth = FakeSignedInAuthController(user),
        ),
        vehicleRepositoryProvider.overrideWithValue(vehicles),
      ],
    );
    container.listen(driverAccessProvider, (_, _) {});
    return container;
  }

  Future<DriverAccess> settledAccess(ProviderContainer container) async {
    await container.read(authControllerProvider.future);
    await container.read(myVehiclesProvider.future);
    return container.read(driverAccessProvider);
  }

  test('quem não é motorista não consulta veículos', () async {
    final vehicles = FakeVehicleRepository();
    final container = containerFor(fakeUser(), vehicles);

    expect(await settledAccess(container), DriverAccess.notDriver);
    expect(vehicles.calls, isEmpty);
  });

  test('motorista sem carro fica bloqueado', () async {
    final container = containerFor(driver(), FakeVehicleRepository());

    expect(await settledAccess(container), DriverAccess.needsVehicle);
  });

  test('cadastrar um carro libera; apagar o único bloqueia de novo', () async {
    final vehicles = FakeVehicleRepository();
    final container = containerFor(driver(), vehicles);
    expect(await settledAccess(container), DriverAccess.needsVehicle);

    final created = await container
        .read(myVehiclesProvider.notifier)
        .add(validVehicleRequest());
    expect(container.read(driverAccessProvider), DriverAccess.ready);

    await container.read(myVehiclesProvider.notifier).remove(created.id);
    expect(container.read(driverAccessProvider), DriverAccess.needsVehicle);
    expect(vehicles.calls, contains('delete:${created.id}'));
  });

  test(
    'carro inválido não é cadastrado e o acesso continua bloqueado',
    () async {
      final vehicles = FakeVehicleRepository();
      final container = containerFor(driver(), vehicles);
      await settledAccess(container);

      await expectLater(
        container
            .read(myVehiclesProvider.notifier)
            .add(validVehicleRequest(renavam: '00000000001')),
        throwsA(isA<InvalidVehicleException>()),
      );
      expect(vehicles.calls, isNot(contains('create')));
      expect(container.read(driverAccessProvider), DriverAccess.needsVehicle);
    },
  );

  test('motorista com carro tem acesso liberado', () async {
    final container = containerFor(driver(), FakeVehicleRepository([onix]));

    expect(await settledAccess(container), DriverAccess.ready);
  });

  test('conta com CNH e PIX sem o papel de motorista: ativa o papel antes do carro', () async {
    // isDriver é true pela CNH + PIX, mas o backend ainda não tem o papel DRIVER
    final halfway = fakeUser().copyWith(
      hasDriverLicense: true,
      pixKey: '12345678909',
    );
    final vehicles = FakeVehicleRepository();
    final container = containerFor(halfway, vehicles);
    await settledAccess(container);

    await container
        .read(myVehiclesProvider.notifier)
        .add(validVehicleRequest());

    expect(auth!.becomeDriverCalls, 1);
    expect(vehicles.calls, contains('create'));
  });

  test('motorista de verdade não chama a ativação de novo', () async {
    final container = containerFor(driver(), FakeVehicleRepository());
    await settledAccess(container);

    await container
        .read(myVehiclesProvider.notifier)
        .add(validVehicleRequest());

    expect(auth!.becomeDriverCalls, 0);
  });

  test(
    'editar um carro atualiza a lista; dados inválidos não chegam à API',
    () async {
      final vehicles = FakeVehicleRepository([onix]);
      final container = containerFor(driver(), vehicles);
      await settledAccess(container);
      final controller = container.read(myVehiclesProvider.notifier);

      await controller.edit(onix.id, validVehicleRequest(model: 'Onix Plus'));
      expect(
        container.read(myVehiclesProvider).value!.single.model,
        'Onix Plus',
      );
      expect(vehicles.calls, contains('update:${onix.id}'));

      await expectLater(
        controller.edit(onix.id, validVehicleRequest(licensePlate: 'XX')),
        throwsA(isA<InvalidVehicleException>()),
      );
      expect(
        container.read(myVehiclesProvider).value!.single.model,
        'Onix Plus',
      );
    },
  );

  test('o formulário de edição vem preenchido com os dados do carro', () {
    final withRenavam = VehicleModel(
      id: 'vehicle-9',
      nickname: 'Carro da facul',
      model: 'Onix',
      licensePlate: 'ABC1D23',
      color: 'Prata',
      type: VehicleType.hatch,
      totalCapacity: 5,
      renavam: validRenavam,
      fuelSources: const [],
    );

    final form = CreateVehicleRequest.fromVehicle(withRenavam);

    expect(form.model, 'Onix');
    expect(form.nickname, 'Carro da facul');
    expect(form.renavam, validRenavam);
    expect(form.licensePlate, 'ABC1D23');
  });
}
