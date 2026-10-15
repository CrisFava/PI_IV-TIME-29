// TODO(backend): o converge-backend ainda não expõe controllers de usuário.
// Ajustar estes caminhos quando os endpoints forem definidos. O contrato
// assumido aqui:
//   POST <createUser>  body: CreateUserRequestDTO  -> ApplicationResponse<CreateUserResponseDTO>
//   GET  <currentUser> header: Bearer token        -> ApplicationResponse<CreateUserResponseDTO>, 404 se não cadastrado
abstract class AuthEndpoints {
  static const String createUser = '/users';
  static const String currentUser = '/users/me';
}
