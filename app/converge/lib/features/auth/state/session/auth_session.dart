import '../../../../core/services/auth/auth_user.dart';
import '../../data/models/user_model.dart';

part 'dto/authenticated.dart';
part 'dto/profile_incomplete.dart';
part 'dto/unauthenticated.dart';

/// Estado da sessão. É a fonte de verdade para os guards de rota.
sealed class AuthSession {
  const AuthSession();
}
