// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/widgets/in_app_notification_card.dart';
import '../../data/models/match_model.dart';

/// Notificação do passageiro: apareceu um motorista compatível enquanto ele estava
/// fora da lista. Tocar volta para a lista, que é onde ele pede a vaga.
class DriverSuggestionAlert extends StatelessWidget {
  final MatchModel match;
  final VoidCallback onOpen;
  final VoidCallback onDismiss;

  const DriverSuggestionAlert({
    super.key,
    required this.match,
    required this.onOpen,
    required this.onDismiss,
  });

  @override
  Widget build(BuildContext context) {
    return InAppNotificationCard(
      key: ValueKey('driver_suggestion_${match.driverRouteId}'),
      icon: Icons.directions_car_rounded,
      title: 'Motorista disponível',
      message: '${match.otherUserName} pode te dar carona. Toque para ver.',
      onTap: onOpen,
      onDismiss: onDismiss,
    );
  }
}
