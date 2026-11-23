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
  /// Id do participante; nulo quando o backend o esconde.
  final String? userId;

  /// Nome do participante.
  final String? name;

  /// Motorista ou passageiro.
  final ParticipationRole? role;

  /// Situação da participação.
  final ParticipantStatus status;

  /// Situação do pagamento; nula para o motorista e quando o backend esconde.
  final PaymentStatus? paymentStatus;

  /// Valor estimado do rateio enquanto o passageiro está na viagem.
  final double? estimatedAmount;

  /// Valor final, definido quando a viagem do passageiro termina (é o valor cobrado).
  final double? finalAmount;

  /// Onde o passageiro embarca e desembarca (o motorista não tem).
  final RoutePlaceModel? boardingLocation;

  /// Onde o passageiro desce.
  final RoutePlaceModel? dropOffLocation;

  /// Quando o passageiro pediu a vaga (os pedidos aparecem do mais antigo ao mais novo).
  final DateTime? requestedAt;

  /// Quando o motorista confirmou que o passageiro entrou no carro.
  final DateTime? boardedAt;

  /// Horário que o passageiro escolheu para sair (HH:mm). Só o motorista recebe.
  final String? requestedDepartureTime;

  /// A vaga foi cancelada pelo motorista ("Remover"), e não pelo passageiro.
  final bool removedByDriver;

  const RideParticipantModel({
    required this.userId,
    required this.name,
    required this.role,
    required this.status,
    this.paymentStatus,
    this.estimatedAmount,
    this.finalAmount,
    this.boardingLocation,
    this.dropOffLocation,
    this.requestedAt,
    this.boardedAt,
    this.requestedDepartureTime,
    this.removedByDriver = false,
  });

  /// Se é o motorista.
  bool get isDriver => role == ParticipationRole.driver;

  /// Se é passageiro.
  bool get isPassenger => role == ParticipationRole.passenger;

  /// Nome para exibir; sem nome, "Motorista" ou "Passageiro".
  String get displayName {
    final value = name?.trim() ?? '';
    return value.isNotEmpty ? value : (isDriver ? 'Motorista' : 'Passageiro');
  }

  /// Lê o participante do JSON da API.
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
      estimatedAmount: (json['estimatedAmount'] as num?)?.toDouble(),
      finalAmount: (json['finalAmount'] as num?)?.toDouble(),
      boardingLocation: _placeOrNull(json['boardingLocation']),
      dropOffLocation: _placeOrNull(json['dropOffLocation']),
      requestedAt: DateTime.tryParse(json['requestedAt'] as String? ?? ''),
      boardedAt: DateTime.tryParse(json['boardedAt'] as String? ?? ''),
      requestedDepartureTime: json['requestedDepartureTime'] as String?,
      removedByDriver: json['removedByDriver'] == true,
    );
  }

  /// Lê um lugar só se o valor for um objeto JSON.
  static RoutePlaceModel? _placeOrNull(Object? json) =>
      json is Map<String, dynamic> ? RoutePlaceModel.fromJson(json) : null;
}
