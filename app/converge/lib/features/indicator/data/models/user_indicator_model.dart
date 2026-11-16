// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

/// Impacto do usuário logado num período, conforme `UserIndicatorResponseDTO`.
@immutable
class UserIndicatorModel {
  /// Período do valor (ex.: `2026-W47`, `2026-11`); vazio no total.
  final String reference;
  final int totalRides;
  final int ridesAsDriver;
  final int ridesAsPassenger;
  final double sharedKm;
  final int carsAvoided;
  final double co2AvoidedKg;
  final double equivalentTrees;

  const UserIndicatorModel({
    this.reference = '',
    required this.totalRides,
    required this.ridesAsDriver,
    required this.ridesAsPassenger,
    required this.sharedKm,
    required this.carsAvoided,
    required this.co2AvoidedKg,
    required this.equivalentTrees,
  });

  factory UserIndicatorModel.fromJson(Map<String, dynamic> json) =>
      UserIndicatorModel(
        reference: json['reference'] as String? ?? '',
        totalRides: (json['totalRides'] as num?)?.toInt() ?? 0,
        ridesAsDriver: (json['ridesAsDriver'] as num?)?.toInt() ?? 0,
        ridesAsPassenger: (json['ridesAsPassenger'] as num?)?.toInt() ?? 0,
        sharedKm: (json['sharedKm'] as num?)?.toDouble() ?? 0,
        carsAvoided: (json['carsAvoided'] as num?)?.toInt() ?? 0,
        co2AvoidedKg: (json['co2AvoidedKg'] as num?)?.toDouble() ?? 0,
        equivalentTrees: (json['equivalentTrees'] as num?)?.toDouble() ?? 0,
      );
}
