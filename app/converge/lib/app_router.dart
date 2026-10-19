import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'app_routes.dart';
import 'features/auth/presentation/pages/complete_profile_page.dart';
import 'features/auth/presentation/pages/forgot_password_page.dart';
import 'features/auth/presentation/pages/home_page.dart';
import 'features/auth/presentation/pages/login_page.dart';
import 'features/auth/presentation/pages/register_page.dart';
import 'features/auth/presentation/pages/splash_page.dart';
import 'features/auth/presentation/pages/welcome_page.dart';
import 'features/auth/state/auth_controller.dart';
import 'features/auth/state/session/auth_session.dart';

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
      GoRoute(
        path: AppRoutes.splash,
        builder: (_, _) => const SplashPage(),
      ),
      GoRoute(
        path: AppRoutes.welcome,
        builder: (_, _) => const WelcomePage(),
      ),
      GoRoute(
        path: AppRoutes.login,
        builder: (_, _) => const LoginPage(),
      ),
      GoRoute(
        path: AppRoutes.register,
        builder: (_, _) => const RegisterPage(),
      ),
      GoRoute(
        path: AppRoutes.forgotPassword,
        builder: (_, _) => const ForgotPasswordPage(),
      ),
      GoRoute(
        path: AppRoutes.completeProfile,
        builder: (_, _) => const CompleteProfilePage(),
      ),
      GoRoute(
        path: AppRoutes.home,
        builder: (_, _) => const HomePage(),
      ),
    ],
  );

  ref.onDispose(() {
    router.dispose();
    session.dispose();
  });
  return router;
});
