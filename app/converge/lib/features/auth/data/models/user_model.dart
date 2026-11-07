// Autor: Cristian Eduardo Fava - RA 25000636

import 'academic_record_model.dart';
import 'participation_role.dart';
import 'pix_key_type.dart';

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
  final String? pixKey;
  final PixKeyType? pixKeyType;
  final bool hasDriverLicense;
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
    this.pixKey,
    this.pixKeyType,
    this.hasDriverLicense = false,
    required this.active,
  });

  bool get isDriver =>
      participationRoles.contains(ParticipationRole.driver) ||
      (hasDriverLicense && pixKey != null && pixKey!.trim().isNotEmpty);

  UserModel copyWith({
    Set<ParticipationRole>? participationRoles,
    String? pixKey,
    PixKeyType? pixKeyType,
    bool? hasDriverLicense,
  }) {
    return UserModel(
      id: id,
      firebaseUid: firebaseUid,
      name: name,
      email: email,
      phone: phone,
      photoUrl: photoUrl,
      participationRoles: participationRoles ?? this.participationRoles,
      academicRecord: academicRecord,
      pixKey: pixKey ?? this.pixKey,
      pixKeyType: pixKeyType ?? this.pixKeyType,
      hasDriverLicense: hasDriverLicense ?? this.hasDriverLicense,
      active: active,
    );
  }

  factory UserModel.fromJson(Map<String, dynamic> json) => UserModel(
    id: json['id'] as String,
    firebaseUid: json['firebaseUid'] as String,
    name: json['name'] as String,
    email: json['email'] as String,
    phone: json['phone'] as String,
    photoUrl: json['photoUrl'] as String?,
    participationRoles: {
      for (final role in (json['participationRoles'] as List? ?? const []))
        ?ParticipationRole.fromJson(role?.toString()),
    },
    academicRecord: AcademicRecordModel.fromJson(
      json['academicRecord'] as Map<String, dynamic>,
    ),
    pixKey: json['pixKey'] as String?,
    pixKeyType: PixKeyType.fromJson(json['pixKeyType'] as String?),
    hasDriverLicense: json['driverLicense'] != null,
    active: json['active'] as bool? ?? true,
  );
}
