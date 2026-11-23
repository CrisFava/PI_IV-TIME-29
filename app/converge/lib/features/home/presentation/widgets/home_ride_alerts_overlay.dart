// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/widgets/in_app_notification_card.dart';
import '../../../matching/presentation/widgets/driver_suggestion_alert.dart';
import '../../../matching/presentation/widgets/incoming_ride_request_alert.dart';
import '../../../matching/state/incoming_ride_request_controller.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../matching/state/ride_cancellation_notice_controller.dart';
import 'passenger_ride_step.dart';

class HomeRideAlertsOverlay extends ConsumerStatefulWidget {
  final bool canDrive;
  final PassengerRideStep passengerStep;
  final int currentTabIndex;
  final bool passengerSheetHidden;
  final bool isPassengerMode;
  final bool showRideRequests;
  final ({String key, String? driverName})? acceptedNotice;
  final VoidCallback onOpenRideSearch;
  final void Function(String offerKey) onOpenOffer;
  final VoidCallback onOpenRidesAsPassenger;
  final VoidCallback? onDismissAcceptedNotice;

  const HomeRideAlertsOverlay({
    super.key,
    required this.canDrive,
    required this.passengerStep,
    required this.currentTabIndex,
    required this.passengerSheetHidden,
    required this.isPassengerMode,
    required this.showRideRequests,
    required this.acceptedNotice,
    required this.onOpenRideSearch,
    required this.onOpenOffer,
    required this.onOpenRidesAsPassenger,
    this.onDismissAcceptedNotice,
  });

  @override
  ConsumerState<HomeRideAlertsOverlay> createState() =>
      _HomeRideAlertsOverlayState();
}

class _HomeRideAlertsOverlayState extends ConsumerState<HomeRideAlertsOverlay> {
  static const Duration _notificationDuration = Duration(seconds: 6);
  final Map<String, DateTime> _notifiedAt = {};

  bool _isNotifying(String key, {required bool onScreen}) {
    final first = _notifiedAt[key];
    if (first == null) {
      if (onScreen) {
        _notifiedAt[key] = DateTime(0);
        return false;
      }
      _notifiedAt[key] = DateTime.now();
      Timer(_notificationDuration, () {
        if (mounted) setState(() {});
      });
      return true;
    }
    return !onScreen &&
        DateTime.now().difference(first) < _notificationDuration;
  }

  void _dismissNotification(String key) {
    setState(() => _notifiedAt[key] = DateTime(0));
  }

  @override
  Widget build(BuildContext context) {
    final matchState = ref.watch(passengerMatchProvider);

    // Passageiro: o motorista mais compatível, enquanto procura carona
    final match = widget.passengerStep == PassengerRideStep.driverList
        ? matchState.suggestion
        : null;
    final onDriverList =
        widget.currentTabIndex == 0 &&
        !widget.passengerSheetHidden &&
        widget.isPassengerMode;
    final matchKey = match == null ? null : 'driver_${match.driverRouteId}';
    final showMatch =
        matchKey != null && _isNotifying(matchKey, onScreen: onDriverList);

    // Motorista: o pedido de vaga mais antigo sem resposta
    final request =
        widget.canDrive ? ref.watch(incomingRideRequestProvider) : null;
    final onRideRequests =
        widget.currentTabIndex == 0 && widget.showRideRequests;
    final requestKey = request == null ? null : 'request_${request.key}';
    final showRequest =
        requestKey != null &&
        _isNotifying(requestKey, onScreen: onRideRequests);

    if (request != null && !showRequest) {
      Future.microtask(
        () => ref.read(incomingRideRequestProvider.notifier).dismiss(request),
      );
    }

    // Os dois lados: a carona ou a vaga cancelada pelo outro (um aviso por vez)
    final cancellation = ref
        .watch(rideCancellationNoticeProvider)
        .firstOrNull;
    final showCancellation =
        cancellation != null &&
        _isNotifying(cancellation.key, onScreen: false);
    if (cancellation != null && !showCancellation) {
      Future.microtask(
        () => ref
            .read(rideCancellationNoticeProvider.notifier)
            .dismiss(cancellation.key),
      );
    }

    // Passageiro: o motorista aceitou o pedido
    final accepted = widget.acceptedNotice;
    final showAccepted =
        accepted != null &&
        _isNotifying(
          accepted.key,
          onScreen: widget.currentTabIndex == 0 && widget.showRideRequests,
        );

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 250),
      child: Column(
        key: ValueKey(
          '${showCancellation ? cancellation.key : ''}|'
          '${showAccepted ? accepted.key : ''}|'
          '${showRequest ? requestKey : ''}|${showMatch ? matchKey : ''}',
        ),
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (showCancellation) ...[
            InAppNotificationCard(
              key: const ValueKey('ride_cancellation_notification'),
              icon: Icons.event_busy_rounded,
              title: cancellation.title,
              message: cancellation.message,
              onTap: () {
                _dismissNotification(cancellation.key);
                ref
                    .read(rideCancellationNoticeProvider.notifier)
                    .dismiss(cancellation.key);
                // Abre a carona em questão: a do motorista ou as do passageiro
                final routeId = cancellation.driverRouteId;
                if (cancellation.forDriver && routeId != null) {
                  widget.onOpenOffer(routeId);
                } else if (!cancellation.forDriver) {
                  widget.onOpenRidesAsPassenger();
                }
              },
              onDismiss: () {
                _dismissNotification(cancellation.key);
                ref
                    .read(rideCancellationNoticeProvider.notifier)
                    .dismiss(cancellation.key);
              },
            ),
            if (showAccepted || showRequest || showMatch)
              const SizedBox(height: 8),
          ],
          if (showAccepted) ...[
            InAppNotificationCard(
              key: const ValueKey('ride_accepted_notification'),
              icon: Icons.check_circle_rounded,
              title: 'Carona confirmada',
              message: accepted.driverName == null
                  ? 'Seu pedido foi aceito. Toque para ver em "Caronas ativas".'
                  : '${accepted.driverName} aceitou seu pedido. Toque para ver em "Caronas ativas".',
              onTap: () {
                _dismissNotification(accepted.key);
                widget.onDismissAcceptedNotice?.call();
                widget.onOpenRidesAsPassenger();
              },
              onDismiss: () {
                _dismissNotification(accepted.key);
                widget.onDismissAcceptedNotice?.call();
              },
            ),
            if (showRequest || showMatch) const SizedBox(height: 8),
          ],
          if (showRequest)
            IncomingRideRequestAlert(
              request: request!,
              onOpen: () {
                _dismissNotification(requestKey);
                widget.onOpenOffer(request.ride.driverRouteId);
              },
              onDismiss: () => _dismissNotification(requestKey),
            ),
          if (showMatch) ...[
            if (showRequest) const SizedBox(height: 8),
            DriverSuggestionAlert(
              match: match!,
              onOpen: () {
                _dismissNotification(matchKey);
                widget.onOpenRideSearch();
              },
              onDismiss: () => _dismissNotification(matchKey),
            ),
          ],
        ],
      ),
    );
  }
}
