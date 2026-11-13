// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/auth/data/models/participation_role.dart';
import 'package:converge/features/auth/data/models/user_model.dart';
import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:converge/features/auth/state/session/auth_session.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Sessão já autenticada com o usuário informado, sem Firebase nem API.
class FakeSignedInAuthController extends AuthController {
  final UserModel user;

  FakeSignedInAuthController(this.user);

  /// Quantas vezes o papel de motorista foi ativado.
  int becomeDriverCalls = 0;

  @override
  Future<AuthSession> build() async => Authenticated(user);

  @override
  Future<UserModel> becomeDriver() async {
    becomeDriverCalls++;
    final driver = user.copyWith(
      participationRoles: {
        ...user.participationRoles,
        ParticipationRole.driver,
      },
    );
    state = AsyncData(Authenticated(driver));
    return driver;
  }
}
