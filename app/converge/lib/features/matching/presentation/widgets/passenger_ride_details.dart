// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_danger_button.dart';
import '../../../../core/widgets/details_section.dart';
import '../../../../core/widgets/details_stat_tile.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../state/passenger_ride_actions_controller.dart';
import '../../state/passenger_rides_provider.dart';
import 'match_date_label.dart';
import 'matching_message.dart';

/// Detalhes de uma viagem em que o usuário vai como passageiro: embarque,
/// desembarque, saída, motorista e carro, o chat e "Sair da viagem" (antes de ela
/// começar). Acompanha a lista, então reflete na hora uma mudança do motorista.
class PassengerRideDetails extends ConsumerStatefulWidget {
  final String rideId;
  final String viewerId;
  final ValueChanged<String> onOpenChat;

  /// Pede a confirmação e sai da viagem; devolve se saiu.
  final Future<bool> Function(RideModel ride) onLeave;

  const PassengerRideDetails({
    super.key,
    required this.rideId,
    required this.viewerId,
    required this.onOpenChat,
    required this.onLeave,
  });

  @override
  ConsumerState<PassengerRideDetails> createState() =>
      _PassengerRideDetailsState();
}

class _PassengerRideDetailsState extends ConsumerState<PassengerRideDetails> {
  // A última versão vista: a lista recarrega sozinha e não deve piscar aqui
  RideModel? _ride;

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';

  Future<void> _leave(RideModel ride) async {
    // A folha antes da espera: depois dela, só fecha se ainda for a da frente
    final sheet = ModalRoute.of(context);
    final left = await widget.onLeave(ride);
    if (!mounted || !left) return;
    if (sheet != null && sheet.isCurrent) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final rides = ref.watch(myPassengerRidesProvider).value;
    if (rides != null) {
      _ride = rides.where((r) => r.id == widget.rideId).firstOrNull;
    }
    final busy = ref.watch(passengerRideActionsProvider);
    final ride = _ride;

    if (ride == null) {
      return const MatchingMessage(
        text: 'Esta viagem não está mais em "Caronas ativas".',
      );
    }

    final own = ride.passengerOf(widget.viewerId);
    final departure = ride.departureDateTime;
    final inProgress = ride.status == RideStatus.inProgress;
    final vehicle = [
      if (ride.vehicleModel?.isNotEmpty ?? false) ride.vehicleModel!,
      if (ride.vehicleLicensePlate?.isNotEmpty ?? false)
        ride.vehicleLicensePlate!,
    ].join(' · ');

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          departure == null ? 'Data a definir' : matchDateTimeLabel(departure),
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
        Text(
          inProgress
              ? 'Viagem em andamento.'
              : 'Sua vaga está confirmada pelo motorista.',
          style: TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: inProgress ? AppColors.success : AppColors.textDark,
          ),
        ),
        DetailsSection(
          title: 'Embarque',
          child: RidePlaceLine(
            icon: Icons.trip_origin_rounded,
            text: (own?.boardingLocation ?? ride.origin).address,
            maxLines: null,
            emphasized: true,
          ),
        ),
        DetailsSection(
          title: 'Desembarque',
          child: RidePlaceLine(
            icon: Icons.flag_rounded,
            text: (own?.dropOffLocation ?? ride.destination).address,
            maxLines: null,
            emphasized: true,
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(top: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (departure != null) ...[
                Expanded(
                  child: DetailsStatTile(
                    icon: Icons.schedule_rounded,
                    value: _hhmm(departure),
                    label: 'Saída',
                  ),
                ),
                const SizedBox(width: 8),
              ],
              Expanded(
                child: DetailsStatTile(
                  icon: Icons.person_rounded,
                  value: ride.driverName,
                  label: 'Motorista',
                ),
              ),
            ],
          ),
        ),
        if (vehicle.isNotEmpty)
          DetailsSection(
            title: 'Carro',
            child: Text(
              vehicle,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
          ),
        const SizedBox(height: 20),
        OutlinedButton.icon(
          key: const ValueKey('passenger_open_chat'),
          onPressed: () {
            HapticFeedback.lightImpact();
            widget.onOpenChat(ride.id);
          },
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.textPrimary,
            side: const BorderSide(color: AppColors.primary),
            padding: const EdgeInsets.symmetric(vertical: 13),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
          icon: const Icon(Icons.chat_bubble_outline_rounded, size: 20),
          label: const Text(
            'Conversar com o motorista',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
        // Sair só antes de a viagem começar
        if (!inProgress)
          DetailsDangerButton(
            key: ValueKey('leave_ride_${ride.id}'),
            label: 'Sair da viagem',
            icon: Icons.logout_rounded,
            isBusy: busy.contains(ride.id),
            onPressed: () => _leave(ride),
          ),
      ],
    );
  }
}
