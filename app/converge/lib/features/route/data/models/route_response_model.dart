// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/foundation.dart';

import 'create_route_request.dart';
import 'route_place_model.dart';
import 'route_recurrence.dart';

@immutable
class RouteResponseModel {
  final String id;
  final AppRouteType type;
  final String status;
  final String? vehicleId;
  final RoutePlaceModel origin;
  final RoutePlaceModel destination;
  final String departureTime;
  final String estimatedArrivalTime;
  final Set<String> weekDays;
  final RouteRecurrence recurrence;

  /// Data da carona de um dia só (`yyyy-MM-dd`); nula na semanal.
  final String? date;
  final double? maxDetourKm;
  final int? totalSeats;
  final int? availableSeats;
  final double? routeDistanceKm;
  final String? encodedPolyline;
  final String? createdAt;
  final String? updatedAt;

  const RouteResponseModel({
    required this.id,
    required this.type,
    required this.status,
    this.vehicleId,
    required this.origin,
    required this.destination,
    required this.departureTime,
    required this.estimatedArrivalTime,
    required this.weekDays,
    this.recurrence = RouteRecurrence.weekly,
    this.date,
    this.maxDetourKm,
    this.totalSeats,
    this.availableSeats,
    this.routeDistanceKm,
    this.encodedPolyline,
    this.createdAt,
    this.updatedAt,
  });

  factory RouteResponseModel.fromJson(Map<String, dynamic> json) {
    final rawWeekDays = json['weekDays'] as List<dynamic>? ?? const [];
    return RouteResponseModel(
      id: json['id'] as String? ?? '',
      type: AppRouteType.fromString(json['type'] as String? ?? 'SEARCH'),
      status: json['status'] as String? ?? '',
      vehicleId: json['vehicleId'] as String?,
      origin: RoutePlaceModel.fromJson(
        (json['origin'] as Map<String, dynamic>?) ?? const {},
      ),
      destination: RoutePlaceModel.fromJson(
        (json['destination'] as Map<String, dynamic>?) ?? const {},
      ),
      departureTime: json['departureTime'] as String? ?? '',
      estimatedArrivalTime: json['estimatedArrivalTime'] as String? ?? '',
      weekDays: rawWeekDays.map((e) => e.toString()).toSet(),
      recurrence: RouteRecurrence.fromApi(json['recurrence'] as String?),
      date: json['date'] as String?,
      maxDetourKm: (json['maxDetourKm'] as num?)?.toDouble(),
      totalSeats: (json['totalSeats'] as num?)?.toInt(),
      availableSeats: (json['availableSeats'] as num?)?.toInt(),
      routeDistanceKm: (json['routeDistanceKm'] as num?)?.toDouble(),
      encodedPolyline: json['encodedPolyline'] as String?,
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
    );
  }
}
