// Autor: Cristian Eduardo Fava - RA 25000636

import '../../data/models/academic_record_model.dart';
import '../../data/models/create_user_request.dart';

/// Dados de perfil enviados ao backend no cadastro.
///
/// Usado tanto no cadastro completo quanto na tela de completar perfil.
class ProfileInput {
  /// Nome completo.
  final String name;

  /// Telefone com DDD.
  final String phone;

  /// Universidade do vínculo acadêmico.
  final String university;

  /// Curso.
  final String course;

  /// Número de matrícula.
  final String enrollmentNumber;

  const ProfileInput({
    required this.name,
    required this.phone,
    required this.university,
    required this.course,
    required this.enrollmentNumber,
  });

  /// Monta o corpo de `POST /users`, tirando os espaços das pontas de cada campo.
  ///
  /// O e-mail vem sempre da conta Firebase, para que os dois não divirjam. Ele também é usado como
  /// e-mail institucional do vínculo acadêmico.
  CreateUserRequest toRequest({
    required String firebaseUid,
    required String email,
  }) => CreateUserRequest(
    firebaseUid: firebaseUid,
    name: name.trim(),
    email: email,
    phone: phone.trim(),
    academicRecord: AcademicRecordModel(
      university: university.trim(),
      course: course.trim(),
      enrollmentNumber: enrollmentNumber.trim(),
      institutionalEmail: email,
    ),
  );
}
