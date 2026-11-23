// Autor: Cristian Eduardo Fava - RA 25000636

import 'verification_method.dart';
import 'verification_status.dart';

/// Espelha o record `AcademicRecord` do backend.
///
/// É o vínculo acadêmico do usuário (RF02, RF03). Só quem está com [status] `VERIFIED` pede vaga ou
/// oferece carona.
class AcademicRecordModel {
  /// Nome da universidade.
  final String university;

  /// Nome do curso.
  final String course;

  /// Número de matrícula.
  final String enrollmentNumber;

  /// E-mail institucional usado na verificação.
  final String institutionalEmail;

  /// URL do documento comprobatório, quando a verificação é por documento.
  final String? documentUrl;

  /// Situação da verificação; todo cadastro nasce `PENDING`.
  final VerificationStatus status;

  /// Como o vínculo é verificado: pelo e-mail institucional (padrão) ou por documento.
  final VerificationMethod verificationMethod;

  /// Motivo da recusa, presente só quando [status] é `REJECTED`.
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

  /// Vínculo já aprovado.
  bool get isVerified => status == VerificationStatus.verified;

  /// Lê o vínculo do JSON da API. Status e método ausentes ou desconhecidos caem nos valores padrão.
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
