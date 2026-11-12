// Autor: Cristian Eduardo Fava - RA 25000636

abstract class AppRoutes {
  static const String splash = '/splash';
  static const String welcome = '/welcome';
  static const String login = '/login';
  static const String register = '/register';
  static const String forgotPassword = '/forgot-password';
  static const String completeProfile = '/complete-profile';
  static const String home = '/';

  /// Conversa entre o motorista e um passageiro de uma viagem.
  static const String chat = '/chat/:rideId/:passengerId';

  static String chatOf({required String rideId, required String passengerId}) =>
      '/chat/$rideId/$passengerId';

  /// Rotas acessíveis sem sessão.
  static const Set<String> public = {welcome, login, register, forgotPassword};
}
