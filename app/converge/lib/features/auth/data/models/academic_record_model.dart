import 'verification_method.dart';
import 'verification_status.dart';

/// Espelha o record `AcademicRecord` do backend.
class AcademicRecordModel {
  final String university;
  final String course;
  final String enrollmentNumber;
  final String institutionalEmail;
  final String? documentUrl;
  final VerificationStatus status;
  final VerificationMethod verificationMethod;
  final String? rejectionReason;

  const AcademicRecordModel({
    required this.university,
    required this.course,
    required this.enrollmentNumber,
    required this.institutionalEmail,
    this.documentUrl,
    this.status = VerificationStatus.pending,
    this.verificationMethod = VerificationMethod.institutionalEmail,
    this.rejectionReason,
  });

  bool get isVerified => status == VerificationStatus.verified;

  factory AcademicRecordModel.fromJson(Map<String, dynamic> json) =>
      AcademicRecordModel(
        university: json['university'] as String,
        course: json['course'] as String,
        enrollmentNumber: json['enrollmentNumber'] as String,
        institutionalEmail: json['institutionalEmail'] as String,
        documentUrl: json['documentUrl'] as String?,
        status: VerificationStatus.fromJson(json['status'] as String?),
        verificationMethod: VerificationMethod.fromJson(
          json['verificationMethod'] as String?,
        ),
        rejectionReason: json['rejectionReason'] as String?,
      );

  /// O backend exige `status` e `verificationMethod` não nulos na criação,
  /// por isso eles sempre vão no payload.
  Map<String, dynamic> toJson() => {
    'university': university,
    'course': course,
    'enrollmentNumber': enrollmentNumber,
    'institutionalEmail': institutionalEmail,
    'documentUrl': documentUrl,
    'status': status.toJson(),
    'verificationMethod': verificationMethod.toJson(),
  };
}
