// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../auth/data/models/participation_role.dart';
import '../../../route/data/models/route_place_model.dart';
import 'participant_status.dart';
import 'payment_status.dart';

/// Um participante da viagem, conforme `ParticipantResponseDTO`.
///
/// O backend esconde o [userId] e os valores de quem o usuário logado não pode
/// ver por completo (ex.: um passageiro olhando outro passageiro).
@immutable
class RideParticipantModel {
  final String? userId;
  final String? name;
  final ParticipationRole? role;
  final ParticipantStatus status;
  final PaymentStatus? paymentStatus;
  final bool paymentConfirmedByDriver;

  /// Onde o passageiro embarca e desembarca (o motorista não tem).
  final RoutePlaceModel? boardingLocation;
  final RoutePlaceModel? dropOffLocation;

  const RideParticipantModel({
    required this.userId,
    required this.name,
    required this.role,
    required this.status,
    this.paymentStatus,
    this.paymentConfirmedByDriver = false,
    this.boardingLocation,
    this.dropOffLocation,
  });

  bool get isDriver => role == ParticipationRole.driver;
  bool get isPassenger => role == ParticipationRole.passenger;

  String get displayName {
    final value = name?.trim() ?? '';
    return value.isNotEmpty ? value : (isDriver ? 'Motorista' : 'Passageiro');
  }

  factory RideParticipantModel.fromJson(Map<String, dynamic> json) {
    final paymentStatus = json['paymentStatus'];
    return RideParticipantModel(
      userId: json['userId'] as String?,
      name: json['name'] as String?,
      role: ParticipationRole.fromJson(json['role'] as String?),
      status: ParticipantStatus.fromJson(json['status']),
      paymentStatus: paymentStatus == null
          ? null
          : PaymentStatus.fromJson(paymentStatus),
      paymentConfirmedByDriver:
          json['paymentConfirmedByDriver'] as bool? ?? false,
      boardingLocation: _placeOrNull(json['boardingLocation']),
      dropOffLocation: _placeOrNull(json['dropOffLocation']),
    );
  }

  static RoutePlaceModel? _placeOrNull(Object? json) =>
      json is Map<String, dynamic> ? RoutePlaceModel.fromJson(json) : null;
}
