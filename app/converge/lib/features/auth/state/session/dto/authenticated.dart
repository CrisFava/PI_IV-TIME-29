part of '../auth_session.dart';

/// Logado no Firebase e com perfil carregado do backend.
class Authenticated extends AuthSession {
  final UserModel user;

  const Authenticated(this.user);
}
