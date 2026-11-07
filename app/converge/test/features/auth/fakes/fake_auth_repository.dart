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

class FakeAuthRepository implements AuthRepository {
  UserModel? stored;
  ApiException? fetchError;
  ApiException? createError;
  ApiException? becomeDriverError;
  ApiException? updateProfileError;
  ApiException? submitDriverLicenseError;
  ApiException? validateCpfError;
  DriverLicenseInput? lastSubmittedLicense;
  CpfValidationResult cpfValidationResult = const CpfValidationResult(
    valid: true,
    message: 'CPF válido.',
  );
  final List<CreateUserRequest> created = [];

  FakeAuthRepository({this.stored});

  @override
  Future<UserModel> fetchCurrentUser() async {
    final error = fetchError;
    if (error != null) throw error;
    final user = stored;
    if (user == null) throw ApiException.fromStatus(404);
    return user;
  }

  @override
  Future<UserModel> createUser(CreateUserRequest request) async {
    created.add(request);
    final error = createError;
    if (error != null) throw error;
    return stored = fakeUser(uid: request.firebaseUid, email: request.email);
  }

  @override
  Future<UserModel> updateProfile({
    required String name,
    required String phone,
    String? photoUrl,
    String? pixKey,
    PixKeyType? pixKeyType,
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

  @override
  Future<UserModel> submitDriverLicense(DriverLicenseInput license) async {
    final error = submitDriverLicenseError;
    if (error != null) throw error;
    lastSubmittedLicense = license;
    return stored ?? fakeUser();
  }

  @override
  Future<CpfValidationResult> validateCpf(String cpf) async {
    final error = validateCpfError;
    if (error != null) throw error;
    return cpfValidationResult;
  }
}
