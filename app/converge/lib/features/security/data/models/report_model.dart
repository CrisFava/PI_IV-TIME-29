// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter/foundation.dart';

import 'report_reason.dart';
import 'report_status.dart';

/// Uma denúncia feita pelo usuário logado, conforme `ReportResponseDTO`.
@immutable
class ReportModel {
  final String id;
  final String reportedId;
  final String? rideId;
  final ReportReason? reason;
  final String? description;
  final ReportStatus? status;
  final DateTime? createdAt;

  const ReportModel({
    required this.id,
    required this.reportedId,
    required this.rideId,
    required this.reason,
    required this.description,
    required this.status,
    required this.createdAt,
  });

  // Lê a resposta de GET /reports/mine. Motivo ou status desconhecido vira null, para um valor novo
  // do backend não quebrar a tela.
  factory ReportModel.fromJson(Map<String, dynamic> json) => ReportModel(
    id: json['id'] as String? ?? '',
    reportedId: json['reportedId'] as String? ?? '',
    rideId: json['rideId'] as String?,
    reason: ReportReason.fromJson(json['reason'] as String?),
    description: json['description'] as String?,
    status: ReportStatus.fromJson(json['status'] as String?),
    createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
  );
}
