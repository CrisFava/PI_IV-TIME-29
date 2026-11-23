// Autor: Cristian Eduardo Fava - RA 25000636

import 'fake_user.dart';

import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/auth/data/models/cpf_validation_result.dart';
import 'package:converge/features/auth/data/models/create_user_request.dart';
import 'package:converge/features/auth/data/models/driver_license_input.dart';
import 'package:converge/features/auth/data/models/participation_role.dart';
import 'package:converge/features/auth/data/models/pix_key_type.dart';
import 'package:converge/features/auth/data/models/user_model.dart';
import 'package:converge/features/auth/data/repository/auth_repository.dart';

/// Estrutura e dados fake em memória para suporte a testes unitários e de widgets.
///
/// Faz o papel da API de usuários: guarda um único perfil em [stored] e deixa cada teste programar um
/// erro por operação (os campos `*Error`), que é lançado na próxima chamada.
class FakeAuthRepository implements AuthRepository {
  /// Perfil "gravado na API"; nulo simula usuário sem cadastro (404 no `fetchCurrentUser`).
  UserModel? stored;

  /// Erro lançado por `fetchCurrentUser`.
  ApiException? fetchError;

  /// Erro lançado por `createUser` (o pedido é registrado em [created] mesmo assim).
  ApiException? createError;

  /// Erro lançado por `becomeDriver`.
  ApiException? becomeDriverError;

  /// Erro lançado por `updateProfile`.
  ApiException? updateProfileError;

  /// Erro lançado por `submitDriverLicense`.
  ApiException? submitDriverLicenseError;

  /// Erro lançado por `validateCpf`.
  ApiException? validateCpfError;

  /// Última CNH recebida, para o teste conferir o que foi enviado.
  DriverLicenseInput? lastSubmittedLicense;

  /// Resposta de `validateCpf`; por padrão, CPF válido.
  CpfValidationResult cpfValidationResult = const CpfValidationResult(
    valid: true,
    message: 'CPF válido.',
  );

  /// Todos os cadastros pedidos, na ordem.
  final List<CreateUserRequest> created = [];

  FakeAuthRepository({this.stored});

  /// Devolve [stored]; sem perfil, lança 404 como a API.
  @override
  Future<UserModel> fetchCurrentUser() async {
    final error = fetchError;
    if (error != null) throw error;
    final user = stored;
    if (user == null) throw ApiException.fromStatus(404);
    return user;
  }

  /// Registra o pedido e grava um perfil falso com o UID e o e-mail recebidos.
  @override
  Future<UserModel> createUser(CreateUserRequest request) async {
    created.add(request);
    final error = createError;
    if (error != null) throw error;
    return stored = fakeUser(uid: request.firebaseUid, email: request.email);
  }

  /// Atualiza nome e telefone, e a foto e o PIX só quando informados (como o backend).
  @override
  Future<UserModel> updateProfile({
    required String name,
    required String phone,
    String? photoUrl,
    String? pixKey,
    PixKeyType? pixKeyType,
    String? expectedFirebaseUid,
  }) async {
    final error = updateProfileError;
    if (error != null) throw error;
    final current = stored ?? fakeUser();
    return stored = UserModel(
      id: current.id,
      firebaseUid: current.firebaseUid,
      name: name,
      email: current.email,
      phone: phone,
      photoUrl: photoUrl ?? current.photoUrl,
      participationRoles: current.participationRoles,
      academicRecord: current.academicRecord,
      pixKey: pixKey ?? current.pixKey,
      pixKeyType: pixKeyType ?? current.pixKeyType,
      active: current.active,
    );
  }

  /// Acrescenta o papel de motorista ao perfil gravado.
  @override
  Future<UserModel> becomeDriver() async {
    final error = becomeDriverError;
    if (error != null) throw error;
    final current = stored ?? fakeUser();
    return stored = UserModel(
      id: current.id,
      firebaseUid: current.firebaseUid,
      name: current.name,
      email: current.email,
      phone: current.phone,
      photoUrl: current.photoUrl,
      participationRoles: {
        ...current.participationRoles,
        ParticipationRole.driver,
      },
      academicRecord: current.academicRecord,
      pixKey: current.pixKey,
      pixKeyType: current.pixKeyType,
      active: current.active,
    );
  }

  /// Guarda a CNH recebida e devolve o perfil sem mudança.
  @override
  Future<UserModel> submitDriverLicense(DriverLicenseInput license) async {
    final error = submitDriverLicenseError;
    if (error != null) throw error;
    lastSubmittedLicense = license;
    return stored ?? fakeUser();
  }

  /// Devolve [cpfValidationResult].
  @override
  Future<CpfValidationResult> validateCpf(String cpf) async {
    final error = validateCpfError;
    if (error != null) throw error;
    return cpfValidationResult;
  }

  /// Resposta de `isEmailAvailable`.
  bool emailAvailable = true;

  /// Erro lançado por `isEmailAvailable`.
  ApiException? emailAvailabilityError;

  /// Devolve [emailAvailable].
  @override
  Future<bool> isEmailAvailable(String email) async {
    final error = emailAvailabilityError;
    if (error != null) throw error;
    return emailAvailable;
  }
}
