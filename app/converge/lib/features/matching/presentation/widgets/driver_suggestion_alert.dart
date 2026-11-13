// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/action_alert_card.dart';
import '../../data/models/match_model.dart';
import 'match_date_label.dart';

/// Aviso do passageiro: o motorista mais compatível com o pedido. Confirmar pede a
/// vaga; recusar mostra o próximo motorista (a lista continua no painel).
class DriverSuggestionAlert extends StatelessWidget {
  final MatchModel match;
  final bool isBusy;
  final VoidCallback onConfirm;
  final VoidCallback onDecline;

  const DriverSuggestionAlert({
    super.key,
    required this.match,
    required this.onConfirm,
    required this.onDecline,
    this.isBusy = false,
  });

  @override
  Widget build(BuildContext context) {
    final vehicle = match.vehicle?.description ?? '';
    final date = match.nextRideDate;
    final when = date == null
        ? 'às ${match.departureTime}'
        : '${matchDateLabel(date)} às ${match.departureTime}';

    return ActionAlertCard(
      key: ValueKey('driver_suggestion_${match.driverRouteId}'),
      icon: Icons.directions_car_rounded,
      accentColor: AppColors.info,
      accentBackground: AppColors.infoBackground,
      title: 'Motorista disponível',
      message: vehicle.isEmpty
          ? '${match.otherUserName} pode te dar carona.'
          : '${match.otherUserName} pode te dar carona ($vehicle).',
      details: [
        (Icons.trip_origin_rounded, 'Sai de ${match.origin.address}, $when'),
        (Icons.flag_rounded, 'Vai até ${match.destination.address}'),
      ],
      primaryLabel: 'Confirmar carona',
      secondaryLabel: 'Recusar',
      isBusy: isBusy,
      onPrimary: onConfirm,
      onSecondary: onDecline,
    );
  }
}
