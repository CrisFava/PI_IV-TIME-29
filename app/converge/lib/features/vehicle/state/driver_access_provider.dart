// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/state/auth_controller.dart';
import '../../auth/state/session/auth_session.dart';
import 'driver_access.dart';
import 'my_vehicles_controller.dart';

/// Acesso de motorista: só fica liberado com pelo menos um carro ativo.
/// Enquanto a lista de carros carrega (ou se ela falhar), o motorista não é bloqueado.
final driverAccessProvider = Provider<DriverAccess>((ref) {
  final session = ref.watch(authControllerProvider).value;
  if (session is! Authenticated || !session.user.isDriver) {
    return DriverAccess.notDriver;
  }

  final vehicles = ref.watch(myVehiclesProvider);
  return switch (vehicles) {
    AsyncData(:final value) when value.isEmpty => DriverAccess.needsVehicle,
    _ => DriverAccess.ready,
  };
});
