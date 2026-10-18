import 'academic_record_model.dart';
import 'participation_role.dart';

/// Perfil do usuário na plataforma, conforme `CreateUserResponseDTO`.
///
/// Inclui só os campos que a autenticação usa; reputação, selos e situação
/// de pagamento ficam para a feature de usuário.
class UserModel {
  final String id;
  final String firebaseUid;
  final String name;
  final String email;
  final String phone;
  final String? photoUrl;
  final Set<ParticipationRole> participationRoles;
  final AcademicRecordModel academicRecord;
  final bool active;

  const UserModel({
    required this.id,
    required this.firebaseUid,
    required this.name,
    required this.email,
    required this.phone,
    required this.photoUrl,
    required this.participationRoles,
    required this.academicRecord,
    required this.active,
  });

  bool get isDriver => participationRoles.contains(ParticipationRole.driver);

  factory UserModel.fromJson(Map<String, dynamic> json) => UserModel(
    id: json['id'] as String,
    firebaseUid: json['firebaseUid'] as String,
    name: json['name'] as String,
    email: json['email'] as String,
    phone: json['phone'] as String,
    photoUrl: json['photoUrl'] as String?,
    participationRoles: {
      for (final role in (json['participationRoles'] as List? ?? const []))
        ?ParticipationRole.fromJson(role as String?),
    },
    academicRecord: AcademicRecordModel.fromJson(
      json['academicRecord'] as Map<String, dynamic>,
    ),
    active: json['active'] as bool? ?? true,
  );
}
