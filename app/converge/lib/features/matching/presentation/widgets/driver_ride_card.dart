// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import 'driver_ride_action_button.dart';
import 'match_date_label.dart';

/// Uma viagem nos detalhes da carona: quando sai, de onde para onde e quem vai
/// (com o atalho para o chat). "Iniciar trajeto" só com passageiro confirmado.
class DriverRideCard extends StatelessWidget {
  final RideModel ride;
  final bool isBusy;
  final VoidCallback onStart;
  final VoidCallback onComplete;

  /// Abre o chat com um passageiro confirmado.
  final ValueChanged<String> onOpenChat;

  const DriverRideCard({
    super.key,
    required this.ride,
    required this.onStart,
    required this.onComplete,
    required this.onOpenChat,
    this.isBusy = false,
  });

  @override
  Widget build(BuildContext context) {
    final confirmed = [
      for (final p in ride.participants)
        if (p.isPassenger &&
            p.status == ParticipantStatus.confirmed &&
            p.userId != null)
          p,
    ];
    final departure = ride.departureDateTime;
    final inProgress = ride.status == RideStatus.inProgress;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: inProgress
            ? AppColors.successBackground
            : AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: inProgress ? AppColors.successBorder : AppColors.borderSubtle,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  departure == null
                      ? 'Data a definir'
                      : matchDateTimeLabel(departure),
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              Text(
                ride.status.label,
                style: TextStyle(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w700,
                  color: inProgress
                      ? AppColors.success
                      : AppColors.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          RidePlaceLine(
            icon: Icons.trip_origin_rounded,
            text: ride.origin.address,
          ),
          const SizedBox(height: 4),
          RidePlaceLine(
            icon: Icons.flag_rounded,
            text: ride.destination.address,
          ),
          for (final passenger in confirmed)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Row(
                children: [
                  const Icon(
                    Icons.person_rounded,
                    size: 16,
                    color: AppColors.textMuted,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      passenger.displayName,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ),
                  IconButton(
                    tooltip: 'Abrir chat com ${passenger.displayName}',
                    visualDensity: VisualDensity.compact,
                    onPressed: () {
                      HapticFeedback.lightImpact();
                      onOpenChat(passenger.userId!);
                    },
                    icon: const Icon(
                      Icons.chat_bubble_rounded,
                      size: 18,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
          const SizedBox(height: 10),
          DriverRideActionButton(
            ride: ride,
            isBusy: isBusy,
            onStart: onStart,
            onComplete: onComplete,
          ),
        ],
      ),
    );
  }
}
