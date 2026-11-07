// Autor: Cristian Eduardo Fava - RA 25000636

import '../models/cpf_validation_result.dart';
import '../models/create_user_request.dart';
import '../models/driver_license_input.dart';
import '../models/pix_key_type.dart';
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

  /// Atualiza dados de perfil do usuário autenticado (`PUT /users/me`).
  Future<UserModel> updateProfile({
    required String name,
    required String phone,
    String? photoUrl,
    String? pixKey,
    PixKeyType? pixKeyType,
  });

  /// Envia dados da Carteira de Motorista (`POST /users/me/driver-license`).
  Future<UserModel> submitDriverLicense(DriverLicenseInput license);

  /// Promove o usuário autenticado a motorista (`POST /users/me/driver`).
  Future<UserModel> becomeDriver();

  /// Valida o CPF através do Socket Server TCP via backend (`POST /users/validate-cpf`).
  Future<CpfValidationResult> validateCpf(String cpf);
}
