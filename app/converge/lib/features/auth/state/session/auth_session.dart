// Autor: Cristian Eduardo Fava - RA 25000636

import '../../../../core/services/auth/auth_user.dart';
import '../../data/models/user_model.dart';

part 'dto/authenticated.dart';
part 'dto/profile_incomplete.dart';
part 'dto/unauthenticated.dart';

/// Estado da sessão. É a fonte de verdade para os guards de rota.
///
/// É `sealed` para o `switch` do redirecionamento cobrir os três casos sem `default`. Cada caso fica
/// em um arquivo `part` dentro de `dto/`.
sealed class AuthSession {
  const AuthSession();
}
