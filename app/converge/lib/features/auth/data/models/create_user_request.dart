import 'academic_record_model.dart';
import 'participation_role.dart';

/// Payload de `CreateUserRequestDTO`.
///
/// O cadastro inicial é sempre como passageiro: virar motorista exige CNH e
/// chave PIX, que são informados depois, na feature de usuário.
class CreateUserRequest {
  final String firebaseUid;
  final String name;
  final String email;
  final String phone;
  final String? photoUrl;
  final Set<ParticipationRole> participationRoles;
  final AcademicRecordModel academicRecord;

  const CreateUserRequest({
    required this.firebaseUid,
    required this.name,
    required this.email,
    required this.phone,
    required this.academicRecord,
    this.photoUrl,
    this.participationRoles = const {ParticipationRole.passenger},
  });

  Map<String, dynamic> toJson() => {
    'firebaseUid': firebaseUid,
    'name': name,
    'email': email,
    'phone': phone,
    'photoUrl': photoUrl,
    'participationRoles': [
      for (final role in participationRoles) role.toJson(),
    ],
    'academicRecord': academicRecord.toJson(),
  };
}
