// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../route/data/models/route_place_model.dart';
import 'ride_participant_model.dart';
import 'ride_status.dart';

/// Uma viagem concreta, conforme `RideResponseDTO`.
@immutable
class RideModel {
  final String id;
  final int sequentialNumber;
  final String driverId;
  final String driverRouteId;
  final RoutePlaceModel origin;
  final RoutePlaceModel destination;
  final DateTime? departureDateTime;
  final DateTime? completedAt;
  final RideStatus status;
  final int totalSeats;
  final int availableSeats;

  /// Sala do chat no Firebase Realtime Database. Fica `null` até o motorista
  /// aceitar o primeiro passageiro.
  final String? chatRoomId;
  final String? vehicleModel;
  final String? vehicleLicensePlate;
  final List<RideParticipantModel> participants;

  const RideModel({
    required this.id,
    required this.sequentialNumber,
    required this.driverId,
    required this.driverRouteId,
    required this.origin,
    required this.destination,
    required this.departureDateTime,
    required this.completedAt,
    required this.status,
    required this.totalSeats,
    required this.availableSeats,
    required this.chatRoomId,
    required this.vehicleModel,
    required this.vehicleLicensePlate,
    required this.participants,
  });

  bool isDrivenBy(String userId) => driverId == userId;

  String get driverName => participants
      .where((participant) => participant.isDriver)
      .map((participant) => participant.displayName)
      .firstWhere((_) => true, orElse: () => 'Motorista');

  /// Passageiros com quem o chat está liberado: os que o motorista aceitou.
  /// Só traz quem o usuário logado pode identificar (o backend esconde o id dos demais).
  List<RideParticipantModel> get passengersWithSeat => [
    for (final participant in participants)
      if (participant.isPassenger &&
          participant.status.hasSeat &&
          participant.userId != null)
        participant,
  ];

  /// A participação de um passageiro. Quem saiu e pediu vaga de novo aparece
  /// mais de uma vez: vale a participação que está com a vaga.
  RideParticipantModel? passengerOf(String userId) {
    final own = participants
        .where((p) => p.isPassenger && p.userId == userId)
        .toList();
    if (own.isEmpty) return null;
    return own.firstWhere((p) => p.status.hasSeat, orElse: () => own.last);
  }

  factory RideModel.fromJson(Map<String, dynamic> json) {
    final vehicle = json['vehicleSnapshot'];
    final participants = json['participants'];
    return RideModel(
      id: json['id'] as String? ?? '',
      sequentialNumber: (json['sequentialNumber'] as num?)?.toInt() ?? 0,
      driverId: json['driverId'] as String? ?? '',
      driverRouteId: json['driverRouteId'] as String? ?? '',
      origin: RoutePlaceModel.fromJson(
        (json['origin'] as Map<String, dynamic>?) ?? const {},
      ),
      destination: RoutePlaceModel.fromJson(
        (json['destination'] as Map<String, dynamic>?) ?? const {},
      ),
      departureDateTime: DateTime.tryParse(
        json['departureDateTime'] as String? ?? '',
      ),
      completedAt: DateTime.tryParse(json['completedAt'] as String? ?? ''),
      status: RideStatus.fromJson(json['status']),
      totalSeats: (json['totalSeats'] as num?)?.toInt() ?? 0,
      availableSeats: (json['availableSeats'] as num?)?.toInt() ?? 0,
      chatRoomId: json['chatRoomId'] as String?,
      vehicleModel: vehicle is Map<String, dynamic>
          ? vehicle['model'] as String?
          : null,
      vehicleLicensePlate: vehicle is Map<String, dynamic>
          ? vehicle['licensePlate'] as String?
          : null,
      participants: [
        if (participants is List)
          for (final participant in participants)
            if (participant is Map<String, dynamic>)
              RideParticipantModel.fromJson(participant),
      ],
    );
  }
}
