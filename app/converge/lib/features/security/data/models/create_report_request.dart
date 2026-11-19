// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter/foundation.dart';

import 'report_reason.dart';

/// Corpo do `POST /reports`. Quem denuncia é sempre o usuário logado.
@immutable
class CreateReportRequest {
  static const int maxDescriptionLength = 500;

  final String reportedId;
  final ReportReason reason;
  final String? description;

  /// Viagem em que aconteceu o problema. Obrigatória para atraso/ausência.
  final String? rideId;

  const CreateReportRequest({
    required this.reportedId,
    required this.reason,
    this.description,
    this.rideId,
  });

  Map<String, dynamic> toJson() {
    final trimmed = description?.trim() ?? '';
    return {
      'reportedId': reportedId,
      'reason': reason.toJson(),
      if (trimmed.isNotEmpty) 'description': trimmed,
      if (rideId != null && rideId!.isNotEmpty) 'rideId': rideId,
    };
  }
}
