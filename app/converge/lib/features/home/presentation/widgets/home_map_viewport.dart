// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/services/maps/google_maps_service.dart';
import '../../../matching/data/models/match_model.dart';
import '../../../matching/state/matching_viewer_provider.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../route/data/models/route_place_model.dart';
import 'home_real_map_background.dart';
import 'passenger_ride_step.dart';
import 'trip_layers_calculator.dart';

/// Componente de mapa interativo da tela inicial exibindo rotas, paradas e motoristas compatíveis.
class HomeMapViewport extends ConsumerStatefulWidget {
  final bool isDriverMode;
  final PassengerRideStep passengerStep;
  final RideModel? shiftRide;
  final LatLng pickupLocation;
  final String pickupAddress;
  final LatLng? destinationLocation;
  final String destinationAddress;
  final List<LatLng>? tripRoutePoints;
  final List<LatLng>? driverApproachRoutePoints;
  final String universityName;
  final void Function(MatchModel driver) onDriverSelected;
  final VoidCallback onMapTap;
  final void Function(LatLng location, String address) onPickupLocationChanged;
  final void Function(LatLng location, String address)
      onDestinationLocationChanged;

  const HomeMapViewport({
    super.key,
    required this.isDriverMode,
    required this.passengerStep,
    required this.shiftRide,
    required this.pickupLocation,
    required this.pickupAddress,
    required this.destinationLocation,
    required this.destinationAddress,
    required this.tripRoutePoints,
    required this.driverApproachRoutePoints,
    required this.universityName,
    required this.onDriverSelected,
    required this.onMapTap,
    required this.onPickupLocationChanged,
    required this.onDestinationLocationChanged,
  });

  @override
  ConsumerState<HomeMapViewport> createState() => _HomeMapViewportState();
}

class _HomeMapViewportState extends ConsumerState<HomeMapViewport> {
  static bool _samePoint(LatLng a, LatLng b) =>
      (a.latitude - b.latitude).abs() < 0.00002 &&
      (a.longitude - b.longitude).abs() < 0.00002;

  static LatLng _latLngOf(RoutePlaceModel place) =>
      LatLng(place.latitude, place.longitude);

  int _geocodeToken = 0;

  @override
  void didUpdateWidget(covariant HomeMapViewport oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.passengerStep != oldWidget.passengerStep) {
      _geocodeToken++;
      final step = widget.passengerStep;
      if (step == PassengerRideStep.dragMapPickup) {
        if (widget.pickupAddress.isEmpty) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted &&
                widget.passengerStep == PassengerRideStep.dragMapPickup) {
              _onCameraCenterChanged(widget.pickupLocation);
            }
          });
        }
      } else if (step == PassengerRideStep.dragMapDestination &&
          widget.destinationLocation != null) {
        if (widget.destinationAddress.isEmpty) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted &&
                widget.passengerStep == PassengerRideStep.dragMapDestination &&
                widget.destinationLocation != null) {
              _onCameraCenterChanged(widget.destinationLocation!);
            }
          });
        }
      }
    }
  }

  void _onCameraCenterChanged(LatLng target) async {
    final token = ++_geocodeToken;
    final activeTarget = target;

    if (widget.passengerStep == PassengerRideStep.dragMapPickup) {
      if (_samePoint(activeTarget, widget.pickupLocation) &&
          widget.pickupAddress.isNotEmpty) {
        return;
      }
      widget.onPickupLocationChanged(activeTarget, '');
      final addr =
          await GoogleMapsService.getAddressFromCoordinates(activeTarget);
      if (mounted &&
          token == _geocodeToken &&
          widget.passengerStep == PassengerRideStep.dragMapPickup) {
        widget.onPickupLocationChanged(activeTarget, addr);
      }
    } else if (widget.passengerStep == PassengerRideStep.dragMapDestination) {
      final destination = widget.destinationLocation;
      if (destination != null &&
          _samePoint(activeTarget, destination) &&
          widget.destinationAddress.isNotEmpty) {
        return;
      }
      widget.onDestinationLocationChanged(activeTarget, '');
      final addr =
          await GoogleMapsService.getAddressFromCoordinates(activeTarget);
      if (mounted &&
          token == _geocodeToken &&
          widget.passengerStep == PassengerRideStep.dragMapDestination) {
        widget.onDestinationLocationChanged(activeTarget, addr);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final shiftRide = widget.shiftRide;
    final showShiftRoute =
        shiftRide != null && widget.passengerStep == PassengerRideStep.idle;
    final viewerId = ref.watch(matchingViewerIdProvider);
    final tripLayers = showShiftRoute
        ? TripLayersCalculator.calculateLayers(shiftRide, viewerId)
        : null;

    final ownTrip =
        showShiftRoute && viewerId != null && !shiftRide.isDrivenBy(viewerId)
            ? shiftRide.passengerOf(viewerId)
            : null;
    final tripEnd = ownTrip == null
        ? shiftRide?.destination
        : (ownTrip.dropOffLocation ?? shiftRide!.destination);
    final tripStart = ownTrip?.boardedAt == null
        ? shiftRide?.origin
        : (ownTrip!.boardingLocation ?? shiftRide!.origin);
    final currentLegStart = tripLayers?.legs
        ?.where((leg) => leg.isCurrent && leg.points.isNotEmpty)
        .firstOrNull
        ?.points
        .first;

    final pickingOnMap =
        widget.passengerStep == PassengerRideStep.dragMapPickup ||
            widget.passengerStep == PassengerRideStep.dragMapDestination;
    final mapBottomPadding =
        widget.passengerStep == PassengerRideStep.routePreview
            ? MediaQuery.sizeOf(context).height * 0.415
            : widget.isDriverMode && !pickingOnMap
                ? MediaQuery.sizeOf(context).height * 0.52
                : 0.0;

    return HomeRealMapBackground(
      isDriverMode: widget.isDriverMode,
      pickupIsPassengerSpot: !widget.isDriverMode &&
          (widget.passengerStep == PassengerRideStep.driverList ||
              widget.passengerStep == PassengerRideStep.waitingDriver),
      showDrivers: !widget.isDriverMode &&
          widget.passengerStep == PassengerRideStep.driverList,
      availableDrivers: ref.watch(passengerMatchProvider).matches,
      acceptedDriver: widget.passengerStep == PassengerRideStep.driverAccepted
          ? ref.watch(passengerMatchProvider).selected
          : null,
      pickupLocation: showShiftRoute
          ? (currentLegStart ?? _latLngOf(tripStart!))
          : widget.pickupLocation,
      destinationLocation: showShiftRoute
          ? _latLngOf(tripEnd!)
          : widget.destinationLocation,
      pickupAddress: showShiftRoute
          ? (currentLegStart != null ? 'Agora' : tripStart!.address)
          : widget.pickupAddress,
      destinationAddress:
          showShiftRoute ? tripEnd!.address : widget.destinationAddress,
      bottomPadding: mapBottomPadding,
      showTrajectory: showShiftRoute ||
          widget.passengerStep == PassengerRideStep.routePreview ||
          widget.passengerStep == PassengerRideStep.routeConfirmation ||
          widget.passengerStep == PassengerRideStep.offerDetails ||
          (!widget.isDriverMode &&
              (widget.passengerStep == PassengerRideStep.driverList ||
                  widget.passengerStep == PassengerRideStep.waitingDriver ||
                  widget.passengerStep ==
                      PassengerRideStep.driverAccepted)),
      tripRoutePoints: showShiftRoute
          ? (tripLayers?.legs == null
              ? TripLayersCalculator.itineraryPoints(shiftRide)
              : null)
          : widget.tripRoutePoints,
      driverApproachRoutePoints: widget.driverApproachRoutePoints,
      tripLegs: tripLayers?.legs,
      tripStops: tripLayers?.stops ?? const [],
      isDraggingPin: widget.passengerStep ==
              PassengerRideStep.dragMapPickup ||
          widget.passengerStep == PassengerRideStep.dragMapDestination,
      centralPinType: widget.passengerStep ==
              PassengerRideStep.dragMapDestination
          ? CentralPinType.destination
          : CentralPinType.pickup,
      onCameraCenterChanged: _onCameraCenterChanged,
      universityName: widget.universityName,
      onDriverSelected: widget.onDriverSelected,
      onMapTap: widget.onMapTap,
    );
  }
}
