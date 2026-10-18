abstract class AppRoutes {
  static const String splash = '/splash';
  static const String welcome = '/welcome';
  static const String login = '/login';
  static const String register = '/register';
  static const String forgotPassword = '/forgot-password';
  static const String completeProfile = '/complete-profile';
  static const String home = '/';

  /// Rotas acessíveis sem sessão.
  static const Set<String> public = {welcome, login, register, forgotPassword};
}
