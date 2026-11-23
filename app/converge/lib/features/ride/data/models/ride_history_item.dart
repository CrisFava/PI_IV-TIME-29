// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../../auth/data/models/participation_role.dart';
import '../../../route/data/models/route_place_model.dart';
import 'payment_status.dart';
import 'ride_status.dart';

/// Uma viagem terminada do histórico, conforme `RideHistoryItemDTO`.
/// Para o passageiro, os locais são o embarque e o desembarque dele e os valores
/// são os dele; para o motorista, o trajeto da carona, o custo total e o que recebeu.
@immutable
class RideHistoryItem {
  final String rideId;
  final bool isDriver;
  final RideStatus status;
  final DateTime? departureDateTime;

  /// Início e fim reais da viagem (sem início guardado, vem o horário marcado).
  final DateTime? startedAt;
  final DateTime? completedAt;
  final RoutePlaceModel origin;
  final RoutePlaceModel destination;
  final String? vehicleModel;
  final int people;
  final double? estimatedAmount;
  final double? paidAmount;
  final PaymentStatus? paymentStatus;

  const RideHistoryItem({
    required this.rideId,
    required this.isDriver,
    required this.status,
    required this.departureDateTime,
    this.startedAt,
    this.completedAt,
    required this.origin,
    required this.destination,
    required this.people,
    this.vehicleModel,
    this.estimatedAmount,
    this.paidAmount,
    this.paymentStatus,
  });

  factory RideHistoryItem.fromJson(Map<String, dynamic> json) {
    final vehicle = json['vehicle'];
    final payment = json['paymentStatus'];
    return RideHistoryItem(
      rideId: json['rideId'] as String? ?? '',
      isDriver:
          ParticipationRole.fromJson(json['role'] as String?) ==
          ParticipationRole.driver,
      status: RideStatus.fromJson(json['rideStatus']),
      departureDateTime: DateTime.tryParse(
        json['departureDateTime'] as String? ?? '',
      ),
      startedAt: DateTime.tryParse(json['startedAt'] as String? ?? ''),
      completedAt: DateTime.tryParse(json['completedAt'] as String? ?? ''),
      origin: RoutePlaceModel.fromJson(
        (json['origin'] as Map<String, dynamic>?) ?? const {},
      ),
      destination: RoutePlaceModel.fromJson(
        (json['destination'] as Map<String, dynamic>?) ?? const {},
      ),
      vehicleModel: vehicle is Map<String, dynamic>
          ? vehicle['model'] as String?
          : null,
      people: (json['people'] as num?)?.toInt() ?? 1,
      estimatedAmount: (json['estimatedAmount'] as num?)?.toDouble(),
      paidAmount: (json['paidAmount'] as num?)?.toDouble(),
      paymentStatus: payment == null ? null : PaymentStatus.fromJson(payment),
    );
  }
}
