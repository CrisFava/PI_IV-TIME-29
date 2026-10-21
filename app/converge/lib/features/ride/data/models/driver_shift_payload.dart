import 'package:flutter/material.dart';

/// DTO estruturado representando os parâmetros de um expediente de motorista,
/// pronto para envio à API do backend Converge.
class DriverShiftPayload {
  final String driverId;
  final String destination;
  final double destinationRadiusKm;
  final double searchRadiusKm;
  final TimeOfDay endSearchTime;
  final DateTime startedAt;

  const DriverShiftPayload({
    required this.driverId,
    required this.destination,
    required this.destinationRadiusKm,
    required this.searchRadiusKm,
    required this.endSearchTime,
    required this.startedAt,
  });

  Map<String, dynamic> toJson() => {
    'driverId': driverId,
    'destination': destination,
    'destinationRadiusKm': destinationRadiusKm,
    'searchRadiusKm': searchRadiusKm,
    'endSearchTime':
        '${endSearchTime.hour.toString().padLeft(2, '0')}:${endSearchTime.minute.toString().padLeft(2, '0')}',
    'startedAt': startedAt.toIso8601String(),
  };

  @override
  String toString() =>
      'DriverShiftPayload(destination: $destination, destRadius: ${destinationRadiusKm}km, searchRadius: ${searchRadiusKm}km, endsAt: ${endSearchTime.format})';
}
