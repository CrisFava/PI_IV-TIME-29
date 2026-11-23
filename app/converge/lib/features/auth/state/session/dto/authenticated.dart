// Autor: Cristian Eduardo Fava - RA 25000636

part of '../auth_session.dart';

/// Logado no Firebase e com perfil carregado do backend.
class Authenticated extends AuthSession {
  /// Perfil do usuário logado, vindo de `GET /users/me`.
  final UserModel user;

  const Authenticated(this.user);
}
