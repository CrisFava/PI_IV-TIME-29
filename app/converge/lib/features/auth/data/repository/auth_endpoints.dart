/// Endpoints do módulo de usuários no converge-backend (`/api/v1/users`).
/// - `POST /users` body: `CreateUserRequestDTO` -> `ApplicationResponse<CreateUserResponseDTO>`
/// - `GET /users/me` header: Bearer token -> `ApplicationResponse<CreateUserResponseDTO>`, 404 se não cadastrado
abstract class AuthEndpoints {
  static const String createUser = '/users';
  static const String currentUser = '/users/me';
}
