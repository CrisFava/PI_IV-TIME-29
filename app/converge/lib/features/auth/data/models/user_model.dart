// Autor: Cristian Eduardo Fava - RA 25000636

import 'academic_record_model.dart';
import 'participation_role.dart';
import 'pix_key_type.dart';

/// Perfil do usuário na plataforma, conforme `CreateUserResponseDTO`.
///
/// Inclui só os campos que a autenticação usa; reputação, selos e situação
/// de pagamento ficam para a feature de usuário.
class UserModel {
  /// Id do usuário no MongoDB, usado como referência nas viagens e no chat.
  final String id;

  /// UID do Firebase da mesma pessoa.
  final String firebaseUid;

  /// Nome completo.
  final String name;

  /// E-mail da conta.
  final String email;

  /// Telefone com DDD.
  final String phone;

  /// Foto de perfil, se houver.
  final String? photoUrl;

  /// Papéis da conta (passageiro e/ou motorista).
  final Set<ParticipationRole> participationRoles;

  /// Vínculo acadêmico e o status da verificação.
  final AcademicRecordModel academicRecord;

  /// Chave PIX para receber dos passageiros; obrigatória para oferecer carona (RF72).
  final String? pixKey;

  /// Tipo da [pixKey].
  final PixKeyType? pixKeyType;

  /// Se a CNH já foi enviada (a API manda o objeto `driverLicense` só quando existe).
  final bool hasDriverLicense;

  /// Conta ativa; falso para conta suspensa ou excluída.
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

  /// Pode atuar como motorista: tem o papel ou já enviou CNH e cadastrou chave PIX.
  bool get isDriver =>
      participationRoles.contains(ParticipationRole.driver) ||
      (hasDriverLicense && pixKey != null && pixKey!.trim().isNotEmpty);

  /// Cópia com os dados de motorista trocados, usada depois de cadastrar CNH, PIX ou virar motorista.
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

  /// Lê o perfil do JSON da API. Papéis desconhecidos são descartados e `active` ausente vale `true`.
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
