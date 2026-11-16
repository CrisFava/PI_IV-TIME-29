// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

/// Impacto de toda a plataforma num período, conforme `PlatformIndicatorResponseDTO`.
@immutable
class PlatformIndicatorModel {
  final int totalRides;
  final int activeUsers;
  final double totalCo2AvoidedKg;

  const PlatformIndicatorModel({
    required this.totalRides,
    required this.activeUsers,
    required this.totalCo2AvoidedKg,
  });

  factory PlatformIndicatorModel.fromJson(Map<String, dynamic> json) =>
      PlatformIndicatorModel(
        totalRides: (json['totalRides'] as num?)?.toInt() ?? 0,
        activeUsers: (json['activeUsers'] as num?)?.toInt() ?? 0,
        totalCo2AvoidedKg: (json['totalCo2AvoidedKg'] as num?)?.toDouble() ?? 0,
      );
}
