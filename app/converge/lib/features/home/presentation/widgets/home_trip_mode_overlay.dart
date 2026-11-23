// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app_routes.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../matching/state/driver_ride_actions_controller.dart';
import '../../../matching/state/matching_viewer_provider.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_stop_type.dart';
import '../../../ride/presentation/widgets/trip_panel.dart';

/// Painel flutuante de controle ativo de viagem para motoristas (embarque, desembarque e encerramento).
class HomeTripModeOverlay extends ConsumerWidget {
  final RideModel ride;
  final double bottomPadding;
  final VoidCallback onClose;
  final void Function(String) onError;

  const HomeTripModeOverlay({
    super.key,
    required this.ride,
    required this.bottomPadding,
    required this.onClose,
    required this.onError,
  });

  Future<void> _runTripAction(
    BuildContext context,
    Future<String?> Function() action,
  ) async {
    final error = await action();
    if (context.mounted && error != null) onError(error);
  }

  Future<void> _confirmTripStep(
    BuildContext context,
    WidgetRef ref,
    RideStopType type,
    List<String> passengerIds,
  ) async {
    final actions = ref.read(driverRideActionsProvider.notifier);
    for (final passengerId in passengerIds) {
      final error = type == RideStopType.pickup
          ? await actions.board(ride.id, passengerId)
          : await actions.arrive(ride.id, passengerId);
      if (error != null) {
        onError(error);
        return;
      }
    }
  }

  Future<void> _markTripNoShow(
    BuildContext context,
    WidgetRef ref,
    String passengerId,
  ) async {
    final name = ride.passengerOf(passengerId)?.displayName ?? 'o passageiro';
    final confirmed = await showConfirmDialog(
      context,
      title: 'Marcar $name como ausente?',
      message:
          'A vaga é liberada e o passageiro não é cobrado nem entra nos indicadores.',
      confirmLabel: 'Marcar ausente',
      destructive: true,
    );
    if (!confirmed || !context.mounted) return;
    await _runTripAction(
      context,
      () => ref
          .read(driverRideActionsProvider.notifier)
          .markNoShow(ride.id, passengerId),
    );
  }

  Future<void> _completeTrip(BuildContext context, WidgetRef ref) async {
    final error =
        await ref.read(driverRideActionsProvider.notifier).complete(ride.id);
    if (!context.mounted) return;
    if (error != null) {
      onError(error);
      return;
    }
    onClose();
    showAppNotice(context, 'Viagem concluída.', type: AppNoticeType.success);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Positioned(
      left: 16,
      right: 16,
      bottom: bottomPadding + 92,
      child: TripPanel(
        ride: ride,
        viewerId: ref.watch(matchingViewerIdProvider) ?? '',
        isBusy: ref.watch(driverRideActionsProvider).contains(ride.id),
        onBoard: (passengerId) => _runTripAction(
          context,
          () => ref
              .read(driverRideActionsProvider.notifier)
              .board(ride.id, passengerId),
        ),
        onArrive: (passengerId) => _runTripAction(
          context,
          () => ref
              .read(driverRideActionsProvider.notifier)
              .arrive(ride.id, passengerId),
        ),
        onConfirmAll: (type, ids) => _confirmTripStep(context, ref, type, ids),
        onNoShow: (passengerId) => _markTripNoShow(context, ref, passengerId),
        onComplete: () => _completeTrip(context, ref),
        onOpenChat: (passengerId) => context.push(
          AppRoutes.chatOf(
            rideId: ride.id,
            passengerId: passengerId,
          ),
        ),
        onClose: onClose,
      ),
    );
  }
}
