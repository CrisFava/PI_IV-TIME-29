import 'package:flutter/foundation.dart';
import 'route_place_model.dart';

enum AppRouteType {
  search('SEARCH'),
  offer('OFFER');

  final String apiValue;
  const AppRouteType(this.apiValue);

  /// Alias para manter compatibilidade com referências a passageiro
  static const AppRouteType passenger = AppRouteType.search;

  static AppRouteType fromString(String value) {
    final upper = value.toUpperCase();
    if (upper == 'PASSENGER' || upper == 'SEARCH') {
      return AppRouteType.search;
    }
    return AppRouteType.values.firstWhere(
      (e) => e.apiValue == upper,
      orElse: () => AppRouteType.search,
    );
  }
}

@immutable
class CreateRouteRequest {
  final AppRouteType type;
  final String? vehicleId;
  final RoutePlaceModel origin;
  final RoutePlaceModel destination;
  final String departureTime;
  final String estimatedArrivalTime;
  final Set<String> weekDays;
  final double? maxDetourKm;
  final int? totalSeats;

  const CreateRouteRequest({
    required this.type,
    this.vehicleId,
    required this.origin,
    required this.destination,
    required this.departureTime,
    required this.estimatedArrivalTime,
    required this.weekDays,
    this.maxDetourKm,
    this.totalSeats,
  });

  Map<String, dynamic> toJson() {
    return {
      'type': type.apiValue,
      if (vehicleId != null) 'vehicleId': vehicleId,
      'origin': origin.toJson(),
      'destination': destination.toJson(),
      'departureTime': departureTime,
      'estimatedArrivalTime': estimatedArrivalTime,
      'weekDays': weekDays.toList(),
      if (maxDetourKm != null) 'maxDetourKm': maxDetourKm,
      if (totalSeats != null) 'totalSeats': totalSeats,
    };
  }
}
