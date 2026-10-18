import '../models/create_user_request.dart';
import '../models/user_model.dart';

/// Chamadas ao backend Spring Boot ligadas à autenticação.
///
/// Erros chegam como `ApiException`.
abstract class AuthRepository {
  /// Perfil do usuário dono do token atual.
  /// Lança `ApiException` com status 404 se ainda não houver
  /// cadastro no backend.
  Future<UserModel> fetchCurrentUser();

  Future<UserModel> createUser(CreateUserRequest request);
}
