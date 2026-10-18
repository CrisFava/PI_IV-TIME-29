import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'features/auth/state/auth_controller.dart';
import 'features/auth/state/session/auth_session.dart';
import 'app_routes.dart';
import 'core/widgets/placeholder_page.dart';

/// Decide para onde o usuário deve ir conforme a sessão. Retorna `null` para manter a rota atual
String? authRedirect(AsyncValue<AuthSession> session, String location) {
  final current = session.value;

  // Restaurando a sessão na abertura do app, ou falha ao restaurá-la.
  if (current == null) {
    return location == AppRoutes.splash ? null : AppRoutes.splash;
  }

  return switch (current) {
    Unauthenticated() =>
      AppRoutes.public.contains(location) ? null : AppRoutes.welcome,

    ProfileIncomplete() =>
      location == AppRoutes.completeProfile || location == AppRoutes.register
          ? null
          : AppRoutes.completeProfile,
    Authenticated() =>
      AppRoutes.public.contains(location) ||
              location == AppRoutes.splash ||
              location == AppRoutes.completeProfile
          ? AppRoutes.home
          : null,
  };
}

final appRouterProvider = Provider<GoRouter>((ref) {
  // O GoRouter é criado uma única vez; mudanças de sessão só reavaliam o
  // redirect através do refreshListenable.
  final session = ValueNotifier(ref.read(authControllerProvider));
  ref.listen(authControllerProvider, (_, next) => session.value = next);

  final router = GoRouter(
    initialLocation: AppRoutes.splash,
    refreshListenable: session,
    redirect: (context, state) =>
        authRedirect(session.value, state.matchedLocation),
    routes: [
      // TODO(ui): trocar os placeholders pelas telas de docs/Telas.
      GoRoute(
        path: AppRoutes.splash,
        builder: (_, _) => const PlaceholderPage('Splash'),
      ),
      GoRoute(
        path: AppRoutes.welcome,
        builder: (_, _) => const PlaceholderPage('Welcome'),
      ),
      GoRoute(
        path: AppRoutes.login,
        builder: (_, _) => const PlaceholderPage('Login'),
      ),
      GoRoute(
        path: AppRoutes.register,
        builder: (_, _) => const PlaceholderPage('Criar conta'),
      ),
      GoRoute(
        path: AppRoutes.forgotPassword,
        builder: (_, _) => const PlaceholderPage('Esqueci minha senha'),
      ),
      GoRoute(
        path: AppRoutes.completeProfile,
        builder: (_, _) => const PlaceholderPage('Completar cadastro'),
      ),
      GoRoute(
        path: AppRoutes.home,
        builder: (_, _) => const PlaceholderPage('Home'),
      ),
    ],
  );

  ref.onDispose(() {
    router.dispose();
    session.dispose();
  });
  return router;
});
