// Autor: Cristian Eduardo Fava - RA 25000636

import 'academic_record_model.dart';
import 'participation_role.dart';

/// Payload de `CreateUserRequestDTO`.
///
/// O cadastro inicial é sempre como passageiro: virar motorista exige CNH e
/// chave PIX, que são informados depois, na feature de usuário.
class CreateUserRequest {
  /// UID da conta recém-criada no Firebase, que liga o cadastro da API ao login.
  final String firebaseUid;

  /// Nome completo.
  final String name;

  /// E-mail da conta.
  final String email;

  /// Telefone no formato `(19) 91234-5678`.
  final String phone;

  /// Foto de perfil, opcional.
  final String? photoUrl;

  /// Papéis da conta; por padrão só passageiro.
  final Set<ParticipationRole> participationRoles;

  /// Vínculo acadêmico informado no cadastro.
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

  /// Corpo de `POST /users`.
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
