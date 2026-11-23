// Autor: Cristian Eduardo Fava - RA 25000636

/// Endpoints do módulo de usuários no converge-backend (`/api/v1/users`).
/// - `POST /users` body: `CreateUserRequestDTO` -> `ApplicationResponse<CreateUserResponseDTO>`
/// - `GET /users/me` header: Bearer token -> `ApplicationResponse<CreateUserResponseDTO>`, 404 se não cadastrado
/// - `POST /users/me/driver` header: Bearer token -> `ApplicationResponse<CreateUserResponseDTO>`
/// - `POST /users/me/driver-license` header: Bearer token -> `ApplicationResponse<CreateUserResponseDTO>`
/// - `POST /users/validate-cpf` body: `CpfValidationRequestDTO` -> `ApplicationResponse<CpfValidationResponseDTO>`
/// - `GET /users/email-availability?email=` -> `ApplicationResponse<{ available }>`
abstract class AuthEndpoints {
  /// Cria o cadastro na API (POST).
  static const String createUser = '/users';

  /// Perfil do usuário logado (GET) e atualização dele (PUT).
  static const String currentUser = '/users/me';

  /// Torna o usuário logado motorista (POST).
  static const String becomeDriver = '/users/me/driver';

  /// Envia a CNH do usuário logado (POST).
  static const String submitDriverLicense = '/users/me/driver-license';

  /// Valida um CPF no servidor de sockets (POST).
  static const String validateCpf = '/users/validate-cpf';

  /// Diz se o e-mail ainda está livre (GET).
  static const String emailAvailability = '/users/email-availability';
}
