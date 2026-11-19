// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';

/// A ação principal de uma viagem do motorista: "Concluir viagem" se ela está em
/// andamento, "Iniciar trajeto" se tem passageiro confirmado, ou o aviso de que
/// falta aceitar alguém.
class DriverRideActionButton extends StatelessWidget {
  final RideModel ride;
  final bool isBusy;
  final VoidCallback onStart;
  final VoidCallback onComplete;

  const DriverRideActionButton({
    super.key,
    required this.ride,
    required this.onStart,
    required this.onComplete,
    this.isBusy = false,
  });

  @override
  Widget build(BuildContext context) {
    if (ride.status == RideStatus.inProgress) {
      return _button(
        key: ValueKey('complete_ride_${ride.id}'),
        label: 'Concluir viagem',
        icon: Icons.flag_circle_rounded,
        color: AppColors.success,
        onPressed: onComplete,
      );
    }

    final hasConfirmed = ride.participants.any(
      (p) => p.isPassenger && p.status == ParticipantStatus.confirmed,
    );
    if (!hasConfirmed) {
      return const Text(
        'Aceite um passageiro para poder iniciar o trajeto.',
        style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
      );
    }

    // "Iniciar trajeto" só de 5 min antes a 5 min depois do horário de saída
    final departure = ride.departureDateTime;
    if (departure != null) {
      final now = DateTime.now();
      final opensAt = departure.subtract(startTolerance);
      if (now.isBefore(opensAt)) {
        return _hint(
          'Você poderá iniciar o trajeto a partir das ${_hhmm(opensAt)}.',
        );
      }
      if (now.isAfter(departure.add(startTolerance))) {
        return _hint(
          'O prazo para iniciar passou: a viagem será cancelada automaticamente.',
        );
      }
    }

    return _button(
      key: ValueKey('start_ride_${ride.id}'),
      label: 'Iniciar trajeto',
      icon: Icons.play_arrow_rounded,
      color: AppColors.primary,
      onPressed: onStart,
    );
  }

  /// Tolerância para iniciar, igual à do backend (`Ride.START_TOLERANCE_MINUTES`).
  static const Duration startTolerance = Duration(minutes: 5);

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';

  Widget _hint(String text) => Text(
    text,
    key: ValueKey('start_ride_hint_${ride.id}'),
    style: const TextStyle(
      fontSize: 12.5,
      fontWeight: FontWeight.w600,
      color: AppColors.textDark,
    ),
  );

  Widget _button({
    required Key key,
    required String label,
    required IconData icon,
    required Color color,
    required VoidCallback onPressed,
  }) {
    return FilledButton.icon(
      key: key,
      onPressed: isBusy
          ? null
          : () {
              HapticFeedback.mediumImpact();
              onPressed();
            },
      style: FilledButton.styleFrom(
        backgroundColor: color,
        foregroundColor: AppColors.textLight,
        disabledBackgroundColor: color,
        disabledForegroundColor: AppColors.textLight,
        padding: const EdgeInsets.symmetric(vertical: 12),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      icon: isBusy
          ? const SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: AppColors.textLight,
              ),
            )
          : Icon(icon, size: 20),
      label: Text(label, style: const TextStyle(fontWeight: FontWeight.w700)),
    );
  }
}
