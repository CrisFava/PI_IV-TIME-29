// Autor: Cristian Eduardo Fava - RA 25000636

/// Rotas nomeadas e mapeamentos de navegação do aplicativo.
abstract class AppRoutes {
  static const String splash = '/splash';
  static const String welcome = '/welcome';
  static const String login = '/login';
  static const String register = '/register';
  static const String forgotPassword = '/forgot-password';
  static const String completeProfile = '/complete-profile';
  static const String home = '/';
  static const String indicators = '/indicators';
  static const String blockedUsers = '/blocked-users';

  /// Conversa entre o motorista e um passageiro de uma viagem.
  static const String chat = '/chat/:rideId/:passengerId';

  static String chatOf({required String rideId, required String passengerId}) =>
      '/chat/$rideId/$passengerId';

  /// Rotas acessíveis sem sessão.
  static const Set<String> public = {welcome, login, register, forgotPassword};
}
