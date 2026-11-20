// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/services/maps/google_maps_service.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/presentation/widgets/trip_panel.dart';
import '../../../ride/state/trip_progress.dart';
import 'home_real_map_background.dart';

class TripLayersCalculator {
  static final Map<String, List<LatLng>> _legPointsCache = {};
  static String? _itineraryPolylineCache;
  static List<LatLng>? _itineraryPointsCache;

  static List<LatLng> legPoints(String polyline) => _legPointsCache.putIfAbsent(
        polyline,
        () => GoogleMapsService.decodePolyline(polyline),
      );

  static List<LatLng>? itineraryPoints(RideModel ride) {
    final polyline = ride.itineraryPolyline;
    if (polyline == null || polyline.isEmpty) return null;
    if (_itineraryPolylineCache != polyline) {
      final points = GoogleMapsService.decodePolyline(polyline);
      _itineraryPolylineCache = polyline;
      _itineraryPointsCache = points;
    }
    return _itineraryPointsCache;
  }

  static ({List<TripMapLeg>? legs, List<TripMapStop> stops}) calculateLayers(
    RideModel ride,
    String? viewerId,
  ) {
    final isDriver = viewerId != null && ride.isDrivenBy(viewerId);
    final progress = TripProgress.of(ride);

    final stops = <TripMapStop>[
      for (var i = progress.currentIndex; i < progress.steps.length; i++)
        TripMapStop(
          position: LatLng(
            progress.steps[i].place.latitude,
            progress.steps[i].place.longitude,
          ),
          title: TripPanel.stopTitle(progress.steps[i]),
          isPickup: progress.steps[i].isPickup,
          isNext: i == progress.currentIndex,
        ),
    ];

    final legPolylines = ride.itineraryLegPolylines;
    if (legPolylines.isEmpty) {
      return (legs: isDriver ? null : const <TripMapLeg>[], stops: stops);
    }

    if (!isDriver) {
      final next = ride.itineraryNextStop;
      final pickup = ride.itineraryMyPickupStop;
      final dropOff = ride.itineraryMyDropOffStop;
      if (next == null || pickup == null || dropOff == null) {
        return (legs: const <TripMapLeg>[], stops: stops);
      }
      final boarded =
          ride.passengerOf(viewerId ?? '')?.boardedAt != null || next > pickup;
      final target = boarded ? dropOff : pickup;
      if (next > target || target >= legPolylines.length) {
        return (legs: const <TripMapLeg>[], stops: stops);
      }
      if (boarded) {
        return (
          legs: [
            TripMapLeg(
              points: [
                for (var i = next; i <= target; i++)
                  ...legPoints(legPolylines[i]),
              ],
              isCurrent: true,
            ),
          ],
          stops: stops,
        );
      }
      return (
        legs: [
          for (var i = next; i <= target; i++)
            TripMapLeg(
              points: legPoints(legPolylines[i]),
              isCurrent: i == next,
            ),
        ],
        stops: stops,
      );
    }

    final legs = progress.legsFor(legPolylines.length);
    return (
      legs: [
        TripMapLeg(
          points: [
            for (final i in legs.current) ...legPoints(legPolylines[i]),
          ],
          isCurrent: true,
        ),
        for (final i in legs.upcoming)
          TripMapLeg(points: legPoints(legPolylines[i]), isCurrent: false),
      ],
      stops: stops,
    );
  }
}
