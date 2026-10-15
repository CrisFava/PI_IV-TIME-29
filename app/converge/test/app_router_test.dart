import 'package:converge/app_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:converge/app_routes.dart';
import 'package:converge/core/services/auth/auth_user.dart';
import 'package:converge/features/auth/state/session/auth_session.dart';

import 'features/auth/fakes/fake_user.dart';

void main() {
  const authUser = AuthUser(
    uid: 'uid',
    email: 'ana@puc.edu.br',
    emailVerified: false,
  );

  group('authRedirect', () {
    test('restaurando a sessão: tudo vai para splash', () {
      const loading = AsyncLoading<AuthSession>();
      expect(authRedirect(loading, AppRoutes.home), AppRoutes.splash);
      expect(authRedirect(loading, AppRoutes.splash), isNull);
    });

    test('falha ao restaurar a sessão: fica na splash', () {
      final error = AsyncError<AuthSession>(Exception(), StackTrace.empty);
      expect(authRedirect(error, AppRoutes.login), AppRoutes.splash);
    });

    test('deslogado: só rotas públicas', () {
      const session = AsyncData<AuthSession>(Unauthenticated());
      expect(authRedirect(session, AppRoutes.home), AppRoutes.welcome);
      expect(authRedirect(session, AppRoutes.splash), AppRoutes.welcome);
      expect(authRedirect(session, AppRoutes.login), isNull);
      expect(authRedirect(session, AppRoutes.register), isNull);
    });

    test('cadastro incompleto: completar perfil', () {
      const session = AsyncData<AuthSession>(ProfileIncomplete(authUser));
      expect(authRedirect(session, AppRoutes.home), AppRoutes.completeProfile);
      expect(authRedirect(session, AppRoutes.login), AppRoutes.completeProfile);
      expect(authRedirect(session, AppRoutes.completeProfile), isNull);
      expect(authRedirect(session, AppRoutes.register), isNull);
    });

    test('autenticado: sai das rotas públicas para home', () {
      final session = AsyncData<AuthSession>(Authenticated(fakeUser()));
      expect(authRedirect(session, AppRoutes.login), AppRoutes.home);
      expect(authRedirect(session, AppRoutes.splash), AppRoutes.home);
      expect(authRedirect(session, AppRoutes.completeProfile), AppRoutes.home);
      expect(authRedirect(session, AppRoutes.home), isNull);
    });
  });
}
