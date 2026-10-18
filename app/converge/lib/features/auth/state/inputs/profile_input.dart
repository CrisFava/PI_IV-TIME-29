import '../../data/models/academic_record_model.dart';
import '../../data/models/create_user_request.dart';

/// Dados de perfil enviados ao backend no cadastro.
class ProfileInput {
  final String name;
  final String phone;
  final String university;
  final String course;
  final String enrollmentNumber;

  const ProfileInput({
    required this.name,
    required this.phone,
    required this.university,
    required this.course,
    required this.enrollmentNumber,
  });

  /// O e-mail vem sempre da conta Firebase, para que os dois não divirjam.
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
