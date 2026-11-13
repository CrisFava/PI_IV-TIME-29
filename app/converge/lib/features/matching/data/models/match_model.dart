// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../../route/data/models/route_place_model.dart';
import 'match_vehicle_model.dart';

/// Uma combinação compatível, conforme `MatchResponseDTO` do backend.
///
/// Os dados de "otherUser" e do trajeto são do outro lado: para o passageiro,
/// do motorista; para o motorista, do passageiro.
@immutable
class MatchModel {
  final String driverRouteId;
  final String passengerRouteId;
  final String otherUserId;
  final String otherUserName;
  final double? otherUserAverageRating;
  final int otherUserTotalReviews;

  /// De 0 a 1. Exibido como porcentagem.
  final double compatibilityScore;
  final double calculatedDetourKm;
  final int scheduleDifferenceMin;
  final Set<String> compatibleDays;
  final double? estimatedCostPerPassenger;

  /// Teto do passageiro (RF35): a estimativa pode subir até aqui se alguém desistir.
  final double? maxAmount;

  /// Próxima data em comum: é a que vai no pedido de vaga.
  final DateTime? nextRideDate;
  final String departureTime;
  final String estimatedArrivalTime;
  final RoutePlaceModel origin;
  final RoutePlaceModel destination;

  /// Só na visão do passageiro.
  final int? availableSeats;
  final MatchVehicleModel? vehicle;

  const MatchModel({
    required this.driverRouteId,
    required this.passengerRouteId,
    required this.otherUserId,
    required this.otherUserName,
    this.otherUserAverageRating,
    this.otherUserTotalReviews = 0,
    required this.compatibilityScore,
    required this.calculatedDetourKm,
    required this.scheduleDifferenceMin,
    this.compatibleDays = const {},
    this.estimatedCostPerPassenger,
    this.maxAmount,
    this.nextRideDate,
    required this.departureTime,
    required this.estimatedArrivalTime,
    required this.origin,
    required this.destination,
    this.availableSeats,
    this.vehicle,
  });

  /// Ex.: `96%`.
  String get compatibilityLabel =>
      '${(compatibilityScore * 100).round().clamp(0, 100)}%';

  bool get canRequestSeat => nextRideDate != null;

  factory MatchModel.fromJson(Map<String, dynamic> json) {
    final days = json['compatibleDays'];
    final vehicle = json['vehicle'];
    final nextDate = json['nextRideDate'];

    return MatchModel(
      driverRouteId: json['driverRouteId'] as String,
      passengerRouteId: json['passengerRouteId'] as String,
      otherUserId: json['otherUserId'] as String,
      otherUserName: json['otherUserName'] as String? ?? 'Usuário',
      otherUserAverageRating: (json['otherUserAverageRating'] as num?)
          ?.toDouble(),
      otherUserTotalReviews:
          (json['otherUserTotalReviews'] as num?)?.toInt() ?? 0,
      compatibilityScore: (json['compatibilityScore'] as num?)?.toDouble() ?? 0,
      calculatedDetourKm: (json['calculatedDetourKm'] as num?)?.toDouble() ?? 0,
      scheduleDifferenceMin:
          (json['scheduleDifferenceMin'] as num?)?.toInt() ?? 0,
      compatibleDays: days is List
          ? days.map((d) => d.toString()).toSet()
          : const {},
      estimatedCostPerPassenger: (json['estimatedCostPerPassenger'] as num?)
          ?.toDouble(),
      maxAmount: (json['maxAmount'] as num?)?.toDouble(),
      nextRideDate: nextDate is String ? DateTime.tryParse(nextDate) : null,
      departureTime: json['departureTime'] as String? ?? '',
      estimatedArrivalTime: json['estimatedArrivalTime'] as String? ?? '',
      origin: RoutePlaceModel.fromJson(json['origin'] as Map<String, dynamic>),
      destination: RoutePlaceModel.fromJson(
        json['destination'] as Map<String, dynamic>,
      ),
      availableSeats: (json['availableSeats'] as num?)?.toInt(),
      vehicle: vehicle is Map<String, dynamic>
          ? MatchVehicleModel.fromJson(vehicle)
          : null,
    );
  }
}
