// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_stat_tile.dart';
import '../../data/models/ride_model.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../../route/data/models/route_response_model.dart';
import '../../data/models/ride_stop_model.dart';
import '../../data/models/ride_stop_type.dart';
import 'ride_place_line.dart';
import 'ride_route_map.dart';

/// O trajeto completo de uma carona do motorista: o mapa com todas as paradas, a
/// distância, a saída, a chegada estimada (contando as paradas) e as paradas na ordem.
/// Sem viagem ainda (ninguém aceito), mostra o caminho da carona.
class RideRouteOverview extends StatelessWidget {
  final RoutePlaceModel origin;
  final RoutePlaceModel destination;
  final String? polyline;
  final double? distanceKm;
  final String departureLabel;
  final String arrivalLabel;
  final int? minutes;
  final List<RideStopModel> stops;
  final Map<String, String> passengerNames;

  const RideRouteOverview._({
    required this.origin,
    required this.destination,
    required this.polyline,
    required this.distanceKm,
    required this.departureLabel,
    required this.arrivalLabel,
    required this.minutes,
    required this.stops,
    required this.passengerNames,
  });

  /// A próxima viagem da carona, com as paradas de quem foi aceito.
  factory RideRouteOverview.ofRide(
    RideModel ride, {
    String? fallbackPolyline,
    double? fallbackDistanceKm,
  }) {
    final departure = ride.departureDateTime;
    final arrival = ride.estimatedArrivalDateTime;
    return RideRouteOverview._(
      origin: ride.origin,
      destination: ride.destination,
      polyline: ride.itineraryPolyline ?? fallbackPolyline,
      distanceKm: ride.itineraryDistanceKm ?? fallbackDistanceKm,
      departureLabel: departure == null ? '—' : _hhmm(departure),
      arrivalLabel: arrival == null ? '—' : '~${_hhmm(arrival)}',
      minutes: departure != null && arrival != null
          ? arrival.difference(departure).inMinutes
          : null,
      stops: ride.itinerary,
      passengerNames: {
        for (final p in ride.participants)
          if (p.isPassenger && p.userId != null) p.userId!: p.displayName,
      },
    );
  }

  /// A carona sem ninguém aceito: só o caminho dela.
  factory RideRouteOverview.ofRoute(RouteResponseModel route) =>
      RideRouteOverview._(
        origin: route.origin,
        destination: route.destination,
        polyline: route.encodedPolyline,
        distanceKm: route.routeDistanceKm,
        departureLabel: route.departureTime.isEmpty ? '—' : route.departureTime,
        arrivalLabel: route.estimatedArrivalTime.isEmpty
            ? '—'
            : '~${route.estimatedArrivalTime}',
        minutes: null,
        stops: const [],
        passengerNames: const {},
      );

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';

  static String _km(double km) => km.toStringAsFixed(1).replaceAll('.', ',');

  static LatLng _latLng(double latitude, double longitude) =>
      LatLng(latitude, longitude);

  @override
  Widget build(BuildContext context) {
    final distance = distanceKm;
    String nameOf(String passengerId) =>
        passengerNames[passengerId] ?? 'Passageiro';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            if (distance != null)
              Expanded(
                child: DetailsStatTile(
                  icon: Icons.route_rounded,
                  value: '${_km(distance)} km',
                  label: 'Percurso',
                ),
              ),
            if (distance != null) const SizedBox(width: 8),
            Expanded(
              child: DetailsStatTile(
                icon: Icons.schedule_rounded,
                value: departureLabel,
                label: 'Saída',
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: DetailsStatTile(
                icon: Icons.flag_rounded,
                value: arrivalLabel,
                label: minutes == null ? 'Chegada' : 'Chegada ($minutes min)',
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        RideRouteMap(
          polyline: polyline,
          origin: _latLng(origin.latitude, origin.longitude),
          destination: _latLng(destination.latitude, destination.longitude),
          stops: [
            for (final stop in stops)
              RouteMapStop(
                position: _latLng(stop.place.latitude, stop.place.longitude),
                title:
                    '${stop.type == RideStopType.pickup ? 'Buscar' : 'Deixar'} '
                    '${nameOf(stop.passengerId)}',
                isPickup: stop.type == RideStopType.pickup,
              ),
          ],
        ),
        const SizedBox(height: 14),
        const Text(
          'PARADAS',
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            letterSpacing: 0.4,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: 6),
        RidePlaceLine(
          icon: Icons.trip_origin_rounded,
          text: 'Saída: ${origin.address}',
          maxLines: 2,
        ),
        for (final stop in stops) ...[
          const SizedBox(height: 6),
          RidePlaceLine(
            icon: stop.type == RideStopType.pickup
                ? Icons.person_pin_circle_rounded
                : Icons.flag_rounded,
            text:
                '${stop.type == RideStopType.pickup ? 'Buscar' : 'Deixar'} '
                '${nameOf(stop.passengerId)}: ${stop.place.address}',
            maxLines: 2,
          ),
        ],
        const SizedBox(height: 6),
        RidePlaceLine(
          icon: Icons.flag_rounded,
          text: 'Destino: ${destination.address}',
          maxLines: 2,
          emphasized: true,
        ),
        if (stops.isEmpty) ...[
          const SizedBox(height: 10),
          const Text(
            'Ainda sem passageiros: as paradas aparecem quando você aceitar alguém.',
            style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
          ),
        ],
      ],
    );
  }
}
