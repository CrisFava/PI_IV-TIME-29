// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/models/participation_role.dart';
import '../../auth/state/auth_controller.dart';
import '../../auth/state/session/auth_session.dart';
import '../data/models/create_vehicle_request.dart';
import '../data/models/vehicle_model.dart';
import '../data/repository/vehicle_repository_impl.dart';
import '../data/validation/vehicle_validator.dart';

/// Veículos ativos do usuário logado. Quem não é motorista não tem veículos,
/// então a lista fica vazia sem chamar a API.
final myVehiclesProvider =
    AsyncNotifierProvider<MyVehiclesController, List<VehicleModel>>(
      MyVehiclesController.new,
    );

class MyVehiclesController extends AsyncNotifier<List<VehicleModel>> {
  @override
  Future<List<VehicleModel>> build() async {
    final session = ref.watch(authControllerProvider).value;
    if (session is! Authenticated || !session.user.isDriver) return const [];

    final vehicles = await ref.read(vehicleRepositoryProvider).listMyVehicles();
    return [
      for (final vehicle in vehicles)
        if (vehicle.active) vehicle,
    ];
  }

  /// Cadastra um veículo. Lança `InvalidVehicleException` (dados inválidos, sem
  /// chamar a API) ou `ApiException` (recusado pelo backend); a lista só muda no sucesso.
  ///
  /// Conta com CNH e PIX mas sem o papel de motorista no backend (ativação que parou
  /// no meio): o papel é ativado antes, porque só motoristas cadastram veículos.
  Future<VehicleModel> add(CreateVehicleRequest request) async {
    // Carro inválido não chega a ativar nada
    VehicleValidator.validate(request);
    await _ensureDriverRole();

    final created = await ref
        .read(vehicleRepositoryProvider)
        .createVehicle(request);
    final current = state.value ?? const <VehicleModel>[];
    state = AsyncData([...current, created]);
    return created;
  }

  Future<void> _ensureDriverRole() async {
    final session = ref.read(authControllerProvider).value;
    if (session is! Authenticated) return;
    if (session.user.participationRoles.contains(ParticipationRole.driver)) {
      return;
    }
    await ref.read(authControllerProvider.notifier).becomeDriver();
  }

  /// Edita um veículo. Lança `InvalidVehicleException` ou `ApiException`;
  /// a lista só muda no sucesso.
  Future<VehicleModel> edit(
    String vehicleId,
    CreateVehicleRequest request,
  ) async {
    final updated = await ref
        .read(vehicleRepositoryProvider)
        .updateVehicle(vehicleId, request);
    final current = state.value ?? const <VehicleModel>[];
    state = AsyncData([
      for (final vehicle in current)
        vehicle.id == vehicleId ? updated : vehicle,
    ]);
    return updated;
  }

  /// Remove um veículo. Sem nenhum veículo, o acesso de motorista fica bloqueado.
  Future<void> remove(String vehicleId) async {
    await ref.read(vehicleRepositoryProvider).deleteVehicle(vehicleId);
    final current = state.value ?? const <VehicleModel>[];
    state = AsyncData([
      for (final vehicle in current)
        if (vehicle.id != vehicleId) vehicle,
    ]);
  }
}
