// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import 'fuel_source_model.dart';
import 'vehicle_type.dart';

/// Veículo do usuário, conforme `VehicleResponseDTO` do backend.
@immutable
class VehicleModel {
  final String id;
  final String? nickname;
  final String model;
  final String licensePlate;
  final String color;
  final VehicleType type;
  final int totalCapacity;
  final List<FuelSourceModel> fuelSources;
  final bool active;

  /// Pode faltar em veículos cadastrados antes do campo existir.
  final String? renavam;

  const VehicleModel({
    required this.id,
    required this.model,
    required this.licensePlate,
    required this.color,
    required this.type,
    required this.totalCapacity,
    this.nickname,
    this.fuelSources = const [],
    this.active = true,
    this.renavam,
  });

  /// Vagas para passageiros: a capacidade menos o motorista.
  int get passengerSeats => totalCapacity > 1 ? totalCapacity - 1 : 1;

  /// Nome para listas: o apelido, se houver, ou o modelo.
  String get displayName =>
      nickname?.trim().isNotEmpty ?? false ? nickname!.trim() : model;

  /// Placa no formato de exibição (ABC-1234 ou ABC1D23).
  String get formattedPlate {
    final plate = licensePlate.toUpperCase();
    final isOldFormat = RegExp(r'^[A-Z]{3}\d{4}$').hasMatch(plate);
    return isOldFormat
        ? '${plate.substring(0, 3)}-${plate.substring(3)}'
        : plate;
  }

  factory VehicleModel.fromJson(Map<String, dynamic> json) => VehicleModel(
    id: json['id'] as String,
    nickname: json['nickname'] as String?,
    model: json['model'] as String? ?? 'Veículo',
    licensePlate: json['licensePlate'] as String? ?? '',
    color: json['color'] as String? ?? '',
    type: VehicleType.fromApi(json['type'] as String?),
    totalCapacity: (json['totalCapacity'] as num?)?.toInt() ?? 2,
    fuelSources: [
      for (final fuel in (json['fuelSources'] as List<dynamic>? ?? const []))
        FuelSourceModel.fromJson(fuel as Map<String, dynamic>),
    ],
    active: json['active'] as bool? ?? true,
    renavam: json['renavam'] as String?,
  );
}
