// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/widgets/in_app_notification_card.dart';
import '../../data/models/driver_ride_request.dart';
import 'match_date_label.dart';

/// Notificação do motorista: um passageiro pediu vaga. Tocar abre a carona, que é
/// onde o pedido é aceito ou recusado.
class IncomingRideRequestAlert extends StatelessWidget {
  final DriverRideRequest request;
  final VoidCallback onOpen;
  final VoidCallback onDismiss;

  const IncomingRideRequestAlert({
    super.key,
    required this.request,
    required this.onOpen,
    required this.onDismiss,
  });

  @override
  Widget build(BuildContext context) {
    final departure = request.ride.departureDateTime;
    final when = departure == null ? '' : ' (${matchDateTimeLabel(departure)})';
    return InAppNotificationCard(
      key: ValueKey('incoming_request_${request.key}'),
      icon: Icons.person_pin_circle_rounded,
      title: 'Pedido de vaga',
      message:
          '${request.passenger.displayName} quer ir com você$when. Toque para responder.',
      onTap: onOpen,
      onDismiss: onDismiss,
    );
  }
}
