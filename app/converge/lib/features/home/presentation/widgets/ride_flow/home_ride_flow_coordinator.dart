// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:converge/features/vehicle/data/models/vehicle_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../route/state/trip_estimate_controller.dart';
import '../passenger_ride_step.dart';
import '../ride_departure_label.dart';
import 'offer_details_step_sheet.dart';
import 'ride_map_pick_card.dart';
import 'ride_place_step_sheet.dart';
import 'ride_route_preview_card.dart';
import 'ride_schedule_step_sheet.dart';

class HomeRideFlowCoordinator extends ConsumerWidget {
  final PassengerRideStep step;
  final bool isDriver;
  final double bottomPadding;
  final String pickupAddress;
  final String destinationAddress;
  final VoidCallback onConfirmPickup;
  final VoidCallback onBackToPickup;
  final VoidCallback onStartDragMapPickup;
  final void Function(String title, LatLng pos) onPickupChosen;
  final VoidCallback onStartDragMapDestination;
  final void Function(String title, LatLng pos) onDestinationChosen;
  final VoidCallback onConfirmDestination;
  final VoidCallback onBackToDestinationSearch;
  final VoidCallback onCancelFlow;
  final VoidCallback onRetryEstimate;
  final VoidCallback onConfirmRoutePreview;
  final VoidCallback onEditDestination;
  final DateTime? offerDeparture;
  final DateTime? rideDeparture;
  final VoidCallback onChangeDeparture;
  final bool offerRepeatWeekly;
  final ValueChanged<bool>? onRepeatWeeklyChanged;
  final VoidCallback onGoToOfferDetails;
  final VoidCallback onReviewAndSubmit;
  final VoidCallback onBackToRoutePreview;
  final List<VehicleModel> vehicles;
  final String? selectedVehicleId;
  final void Function(VehicleModel) onVehicleChanged;
  final int seats;
  final void Function(int) onSeatsChanged;
  final double searchRadiusKm;
  final void Function(double) onSearchRadiusChanged;
  final bool isSubmitting;
  final VoidCallback onAddVehicle;
  final VoidCallback onBackToRouteConfirmation;

  const HomeRideFlowCoordinator({
    super.key,
    required this.step,
    required this.isDriver,
    required this.bottomPadding,
    required this.pickupAddress,
    required this.destinationAddress,
    required this.onConfirmPickup,
    required this.onBackToPickup,
    required this.onStartDragMapPickup,
    required this.onPickupChosen,
    required this.onStartDragMapDestination,
    required this.onDestinationChosen,
    required this.onConfirmDestination,
    required this.onBackToDestinationSearch,
    required this.onCancelFlow,
    required this.onRetryEstimate,
    required this.onConfirmRoutePreview,
    required this.onEditDestination,
    required this.offerDeparture,
    required this.rideDeparture,
    required this.onChangeDeparture,
    required this.offerRepeatWeekly,
    required this.onRepeatWeeklyChanged,
    required this.onGoToOfferDetails,
    required this.onReviewAndSubmit,
    required this.onBackToRoutePreview,
    required this.vehicles,
    required this.selectedVehicleId,
    required this.onVehicleChanged,
    required this.seats,
    required this.onSeatsChanged,
    required this.searchRadiusKm,
    required this.onSearchRadiusChanged,
    required this.isSubmitting,
    required this.onAddVehicle,
    required this.onBackToRouteConfirmation,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tripEstimate = ref.watch(tripEstimateProvider);

    return switch (step) {
      PassengerRideStep.dragMapPickup => RideMapPickCard(
        isOrigin: true,
        address: pickupAddress,
        onConfirm: onConfirmPickup,
        onBack: onBackToPickup,
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.searchDestination => RidePlaceStepSheet(
        key: const ValueKey('ride_flow_destination'),
        isOrigin: false,
        isDriver: isDriver,
        selectedAddress: destinationAddress,
        onPickOnMap: onStartDragMapDestination,
        onPlaceChosen: onDestinationChosen,
        onContinue: onConfirmDestination,
        onBack: onBackToPickup,
        onClose: onCancelFlow,
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.dragMapDestination => RideMapPickCard(
        isOrigin: false,
        address: destinationAddress,
        onConfirm: onConfirmDestination,
        onBack: onBackToDestinationSearch,
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.routePreview => RideRoutePreviewCard(
        key: const ValueKey('ride_flow_route_preview'),
        originAddress: pickupAddress,
        destinationAddress: destinationAddress,
        estimate: tripEstimate.estimate,
        isEstimating: tripEstimate.isLoading,
        onConfirm: onConfirmRoutePreview,
        onRetry: onRetryEstimate,
        onEdit: onEditDestination,
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.routeConfirmation => RideScheduleStepSheet(
        isDriver: isDriver,
        departureLabel: rideDepartureLabel(
          isDriver ? offerDeparture : rideDeparture,
          DateTime.now(),
        ),
        hasDeparture: (isDriver ? offerDeparture : rideDeparture) != null,
        onChangeDeparture: onChangeDeparture,
        repeatWeekly: offerRepeatWeekly,
        onRepeatWeeklyChanged: onRepeatWeeklyChanged,
        isSubmitting: false,
        onSubmit: isDriver ? onGoToOfferDetails : onReviewAndSubmit,
        onBack: onBackToRoutePreview,
        onClose: onCancelFlow,
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.offerDetails => OfferDetailsStepSheet(
        key: const ValueKey('ride_flow_offer_details'),
        vehicles: vehicles,
        selectedVehicleId: selectedVehicleId,
        onVehicleChanged: onVehicleChanged,
        seats: seats,
        onSeatsChanged: onSeatsChanged,
        searchRadiusKm: searchRadiusKm,
        onSearchRadiusChanged: onSearchRadiusChanged,
        isSubmitting: isSubmitting,
        onSubmit: onReviewAndSubmit,
        onAddVehicle: onAddVehicle,
        onBack: onBackToRouteConfirmation,
        onClose: onCancelFlow,
        bottomSafeArea: bottomPadding,
      ),
      _ => RidePlaceStepSheet(
        key: const ValueKey('ride_flow_origin'),
        isOrigin: true,
        isDriver: isDriver,
        selectedAddress: pickupAddress,
        onPickOnMap: onStartDragMapPickup,
        onPlaceChosen: onPickupChosen,
        onContinue: onConfirmPickup,
        onClose: onCancelFlow,
        bottomSafeArea: bottomPadding,
      ),
    };
  }
}
