// Autor: Cristian Eduardo Fava - RA 25000636

/// Endpoints do módulo de usuários no converge-backend (`/api/v1/users`).
/// - `POST /users` body: `CreateUserRequestDTO` -> `ApplicationResponse<CreateUserResponseDTO>`
/// - `GET /users/me` header: Bearer token -> `ApplicationResponse<CreateUserResponseDTO>`, 404 se não cadastrado
/// - `POST /users/me/driver` header: Bearer token -> `ApplicationResponse<CreateUserResponseDTO>`
/// - `POST /users/me/driver-license` header: Bearer token -> `ApplicationResponse<CreateUserResponseDTO>`
/// - `POST /users/validate-cpf` body: `CpfValidationRequestDTO` -> `ApplicationResponse<CpfValidationResponseDTO>`
abstract class AuthEndpoints {
  static const String createUser = '/users';
  static const String currentUser = '/users/me';
  static const String becomeDriver = '/users/me/driver';
  static const String submitDriverLicense = '/users/me/driver-license';
  static const String validateCpf = '/users/validate-cpf';
}
