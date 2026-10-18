part of '../auth_session.dart';

/// Logado no Firebase, mas sem cadastro no backend: o `POST` de cadastro
/// falhou depois de a conta Firebase ser criada, ou a conta foi criada fora
/// do app. O usuário precisa completar o perfil com [ProfileInput].
class ProfileIncomplete extends AuthSession {
  final AuthUser authUser;

  const ProfileIncomplete(this.authUser);
}
