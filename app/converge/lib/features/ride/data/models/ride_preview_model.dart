// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import 'ride_stop_model.dart';

/// Como a viagem ficaria aceitando um pedido, conforme `RidePreviewResponseDTO`:
/// o caminho de agora e o caminho com o passageiro (polylines do Google, nulas sem
/// o Google) e quanto ele acrescenta.
@immutable
class RidePreviewModel {
  final String? currentPolyline;
  final String? withPassengerPolyline;
  final double extraKm;

  /// Distância total da viagem passando pelo passageiro.
  final double withPassengerDistanceKm;

  /// A viagem como ficaria: o tempo total e as paradas na ordem, com o passageiro.
  final int? withPassengerDurationMinutes;
  final List<RideStopModel> withPassengerStops;
  final int? extraMinutes;

  /// O caminho com o passageiro em trechos (um até cada parada e o último até o
  /// destino) e o tempo de cada um, em segundos; vazios sem o Google.
  final List<String> withPassengerLegPolylines;
  final List<int> withPassengerLegSeconds;

  const RidePreviewModel({
    required this.currentPolyline,
    required this.withPassengerPolyline,
    required this.extraKm,
    required this.withPassengerDistanceKm,
    this.withPassengerDurationMinutes,
    this.withPassengerStops = const [],
    required this.extraMinutes,
    this.withPassengerLegPolylines = const [],
    this.withPassengerLegSeconds = const [],
  });

  factory RidePreviewModel.fromJson(Map<String, dynamic> json) =>
      RidePreviewModel(
        currentPolyline: json['currentPolyline'] as String?,
        withPassengerPolyline: json['withPassengerPolyline'] as String?,
        extraKm: (json['extraKm'] as num?)?.toDouble() ?? 0,
        withPassengerDistanceKm:
            (json['withPassengerDistanceKm'] as num?)?.toDouble() ?? 0,
        withPassengerDurationMinutes:
            (json['withPassengerDurationMinutes'] as num?)?.toInt(),
        withPassengerStops: [
          if (json['withPassengerStops'] case final List stops)
            for (final stop in stops)
              if (stop is Map<String, dynamic>) RideStopModel.fromJson(stop),
        ],
        extraMinutes: (json['extraMinutes'] as num?)?.toInt(),
        withPassengerLegPolylines: [
          if (json['withPassengerLegPolylines'] case final List legs)
            for (final leg in legs)
              if (leg is String) leg,
        ],
        withPassengerLegSeconds: [
          if (json['withPassengerLegSeconds'] case final List seconds)
            for (final value in seconds)
              if (value is num) value.toInt(),
        ],
      );
}
