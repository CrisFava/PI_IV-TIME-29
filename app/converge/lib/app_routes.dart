// Autor: Cristian Eduardo Fava - RA 25000636

/// Rotas nomeadas e mapeamentos de navegação do aplicativo.
///
/// Os caminhos ficam só aqui; o `app_router.dart` liga cada um à sua tela e decide para onde mandar o
/// usuário conforme a sessão.
abstract class AppRoutes {
  /// Tela de carregamento enquanto a sessão do Firebase ainda é lida.
  static const String splash = '/splash';

  /// Boas-vindas, primeira tela de quem não está logado.
  static const String welcome = '/welcome';

  /// Login com e-mail e senha.
  static const String login = '/login';

  /// Cadastro de conta nova.
  static const String register = '/register';

  /// Recuperação de senha por e-mail.
  static const String forgotPassword = '/forgot-password';

  /// Complemento do perfil para quem tem conta no Firebase mas ainda não tem cadastro na API.
  static const String completeProfile = '/complete-profile';

  /// Tela principal, com a navegação por abas.
  static const String home = '/';

  /// Indicadores de mobilidade e sustentabilidade do usuário.
  static const String indicators = '/indicators';

  /// Lista de usuários bloqueados pelo usuário logado.
  static const String blockedUsers = '/blocked-users';

  /// Conversa entre o motorista e um passageiro de uma viagem.
  static const String chat = '/chat/:rideId/:passengerId';

  /// Caminho do chat já preenchido, para usar com `context.push`.
  static String chatOf({required String rideId, required String passengerId}) =>
      '/chat/$rideId/$passengerId';

  /// Rotas acessíveis sem sessão.
  static const Set<String> public = {welcome, login, register, forgotPassword};
}
