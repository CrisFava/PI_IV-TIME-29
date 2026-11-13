// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/action_alert_card.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/incoming_ride_request_controller.dart';
import '../../state/ride_request_actions_controller.dart';
import 'match_date_label.dart';

/// Aviso do motorista em expediente: um passageiro pediu vaga. Aceitar ou recusar
/// responde o pedido; depois aparece o próximo, se houver.
class IncomingRideRequestAlert extends ConsumerWidget {
  const IncomingRideRequestAlert({super.key});

  Future<void> _respond(
    BuildContext context,
    WidgetRef ref,
    DriverRideRequest request, {
    required bool accept,
  }) async {
    final actions = ref.read(rideRequestActionsProvider.notifier);
    final error = accept
        ? await actions.accept(request)
        : await actions.reject(request);

    if (error != null) {
      if (context.mounted) {
        showAppNotice(context, error, type: AppNoticeType.error);
      }
      return;
    }
    ref.read(incomingRideRequestProvider.notifier).dismiss(request);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final request = ref.watch(incomingRideRequestProvider);
    if (request == null) return const SizedBox.shrink();

    final isBusy = ref.watch(rideRequestActionsProvider).contains(request.key);
    final passenger = request.passenger;
    final boarding = passenger.boardingLocation?.address ?? '';
    final dropOff = passenger.dropOffLocation?.address ?? '';
    final departure = request.ride.departureDateTime;

    return ActionAlertCard(
      key: ValueKey('incoming_request_${request.key}'),
      icon: Icons.person_pin_circle_rounded,
      accentColor: AppColors.success,
      accentBackground: AppColors.successBackground,
      title: 'Pedido de vaga',
      message: '${passenger.displayName} quer ir com você.',
      details: [
        if (boarding.isNotEmpty)
          (Icons.trip_origin_rounded, 'Embarca em $boarding'),
        if (dropOff.isNotEmpty) (Icons.flag_rounded, 'Desce em $dropOff'),
        if (departure != null)
          (Icons.schedule_rounded, matchDateTimeLabel(departure)),
      ],
      primaryLabel: 'Aceitar',
      secondaryLabel: 'Recusar',
      isBusy: isBusy,
      onPrimary: () => _respond(context, ref, request, accept: true),
      onSecondary: () => _respond(context, ref, request, accept: false),
    );
  }
}
