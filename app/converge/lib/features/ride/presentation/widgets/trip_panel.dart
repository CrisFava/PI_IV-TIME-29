// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/slide_to_confirm.dart';
import '../../data/models/participant_status.dart';
import '../../data/models/ride_model.dart';
import '../../data/models/ride_participant_model.dart';
import '../../data/models/ride_stop_type.dart';
import '../../state/trip_progress.dart';
import 'ride_place_line.dart';

/// Modo viagem: o card da viagem em andamento, com as ações de quem está olhando.
///
/// O motorista vê a próxima parada e confirma, deslizando, quem entrou ou desceu
/// (sem localização real, é isso que faz a viagem avançar). O passageiro acompanha
/// a própria situação e fala com o motorista.
class TripPanel extends StatelessWidget {
  final RideModel ride;
  final String viewerId;
  final bool isBusy;
  final ValueChanged<String> onBoard;
  final ValueChanged<String> onNoShow;
  final ValueChanged<String> onArrive;

  /// "Todos entraram/desceram": confirma os pendentes da parada, um depois do outro.
  final void Function(RideStopType type, List<String> passengerIds)
  onConfirmAll;
  final VoidCallback onComplete;

  /// Conversa com o passageiro (motorista) ou com o motorista (passageiro).
  final ValueChanged<String> onOpenChat;
  final VoidCallback onClose;

  const TripPanel({
    super.key,
    required this.ride,
    required this.viewerId,
    required this.isBusy,
    required this.onBoard,
    required this.onNoShow,
    required this.onArrive,
    required this.onConfirmAll,
    required this.onComplete,
    required this.onOpenChat,
    required this.onClose,
  });

  @override
  Widget build(BuildContext context) {
    final isDriver = ride.isDrivenBy(viewerId);
    return Container(
      key: const ValueKey('trip_panel'),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: AppColors.successBorder),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: 0.12),
            blurRadius: 24,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                width: 10,
                height: 10,
                decoration: const BoxDecoration(
                  color: AppColors.success,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Viagem em andamento',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              IconButton(
                key: const ValueKey('trip_panel_close'),
                tooltip: 'Voltar ao mapa',
                visualDensity: VisualDensity.compact,
                onPressed: () {
                  HapticFeedback.lightImpact();
                  onClose();
                },
                icon: const Icon(
                  Icons.close_rounded,
                  size: 20,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          if (isDriver) _driverContent() else _passengerContent(),
        ],
      ),
    );
  }

  // --------------------------------------------------------------- motorista

  Widget _driverContent() {
    final progress = TripProgress.of(ride);
    final step = progress.current;

    if (step == null) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Todas as paradas foram feitas. Ao chegar ao destino, conclua a viagem.',
            style: TextStyle(fontSize: 13.5, color: AppColors.textDark),
          ),
          const SizedBox(height: 8),
          RidePlaceLine(
            icon: Icons.flag_rounded,
            text: ride.destination.address,
            maxLines: 2,
            emphasized: true,
          ),
          const SizedBox(height: 12),
          SlideToConfirm(
            key: const ValueKey('trip_complete_slider'),
            label: 'Deslize para concluir a viagem',
            color: AppColors.success,
            isBusy: isBusy,
            onConfirmed: onComplete,
          ),
        ],
      );
    }

    final pending = [
      for (final passenger in step.passengers)
        if (!_passengerDone(step.type, passenger)) passenger,
    ];
    final verb = step.isPickup ? 'entrou' : 'desceu';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'Parada ${progress.currentIndex + 1} de ${progress.steps.length} · '
          '${step.isPickup ? 'Embarque' : 'Desembarque'}',
          style: const TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w700,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          stopTitle(step),
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 6),
        RidePlaceLine(
          icon: step.isPickup
              ? Icons.person_pin_circle_rounded
              : Icons.flag_rounded,
          text: step.place.address,
          maxLines: 2,
          emphasized: true,
        ),
        const SizedBox(height: 10),
        for (final passenger in step.passengers)
          _PassengerRow(
            passenger: passenger,
            done: _passengerDone(step.type, passenger),
            // Com mais de um na parada, cada um pode ser confirmado sozinho
            actionLabel: step.passengers.length > 1 ? _capitalized(verb) : null,
            isBusy: isBusy,
            onAction: () => _confirm(step.type, passenger.userId!),
            onNoShow: step.isPickup ? () => onNoShow(passenger.userId!) : null,
            onChat: () => onOpenChat(passenger.userId!),
          ),
        const SizedBox(height: 10),
        if (pending.isNotEmpty)
          SlideToConfirm(
            key: ValueKey('trip_step_slider_${step.firstStop}'),
            label: pending.length == 1
                ? '${_firstName(pending.single)} $verb'
                : 'Todos ${step.isPickup ? 'entraram' : 'desceram'}',
            color: step.isPickup ? AppColors.primary : AppColors.success,
            isBusy: isBusy,
            onConfirmed: () => onConfirmAll(step.type, [
              for (final passenger in pending) passenger.userId!,
            ]),
          ),
      ],
    );
  }

  /// "Parada de Ana" ou, com mais gente no mesmo ponto, "Parada de Ana/Bruno/Carla".
  static String stopTitle(TripStep step) =>
      'Parada de ${step.passengers.map(_firstName).join('/')}';

  void _confirm(RideStopType type, String passengerId) =>
      type == RideStopType.pickup
      ? onBoard(passengerId)
      : onArrive(passengerId);

  static bool _passengerDone(RideStopType type, RideParticipantModel p) {
    final confirmed = p.status == ParticipantStatus.confirmed;
    return type == RideStopType.pickup
        ? !confirmed || p.boardedAt != null
        : !confirmed;
  }

  static String _firstName(RideParticipantModel p) =>
      p.displayName.split(' ').first;

  static String _capitalized(String text) =>
      text.isEmpty ? text : '${text[0].toUpperCase()}${text.substring(1)}';

  // --------------------------------------------------------------- passageiro

  Widget _passengerContent() {
    final me = ride.passengerOf(viewerId);
    final boarded = me?.boardedAt != null;
    final arrived = me?.status == ParticipantStatus.completed;
    final vehicle = [
      if (ride.vehicleModel?.isNotEmpty ?? false) ride.vehicleModel!,
      if (ride.vehicleLicensePlate?.isNotEmpty ?? false)
        ride.vehicleLicensePlate!,
    ].join(' · ');

    final (status, place, icon) = arrived
        ? (
            'Você chegou. A cobrança da carona chega em instantes.',
            me?.dropOffLocation?.address ?? ride.destination.address,
            Icons.flag_rounded,
          )
        : boarded
        ? (
            'Você está a bordo. Desembarque:',
            me?.dropOffLocation?.address ?? ride.destination.address,
            Icons.flag_rounded,
          )
        : (
            'O motorista está a caminho do seu embarque:',
            me?.boardingLocation?.address ?? ride.origin.address,
            Icons.trip_origin_rounded,
          );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          vehicle.isEmpty ? ride.driverName : '${ride.driverName} · $vehicle',
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          status,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w700,
            color: arrived || boarded
                ? AppColors.success
                : AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 6),
        RidePlaceLine(icon: icon, text: place, maxLines: 2, emphasized: true),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          key: const ValueKey('trip_passenger_chat'),
          onPressed: () {
            HapticFeedback.lightImpact();
            onOpenChat(viewerId);
          },
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.textPrimary,
            side: const BorderSide(color: AppColors.primary),
            padding: const EdgeInsets.symmetric(vertical: 12),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
          icon: const Icon(Icons.chat_bubble_outline_rounded, size: 19),
          label: const Text(
            'Conversar com o motorista',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
      ],
    );
  }
}

/// Um passageiro da parada: nome, chat, "Não apareceu" (no embarque) e, com
/// vários na mesma parada, a confirmação só dele.
class _PassengerRow extends StatelessWidget {
  final RideParticipantModel passenger;
  final bool done;
  final String? actionLabel;
  final bool isBusy;
  final VoidCallback onAction;
  final VoidCallback? onNoShow;
  final VoidCallback onChat;

  const _PassengerRow({
    required this.passenger,
    required this.done,
    required this.actionLabel,
    required this.isBusy,
    required this.onAction,
    required this.onNoShow,
    required this.onChat,
  });

  @override
  Widget build(BuildContext context) {
    final name = passenger.displayName;
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        children: [
          CircleAvatar(
            radius: 16,
            backgroundColor: done
                ? AppColors.successBackground
                : AppColors.neutralLight,
            child: done
                ? const Icon(
                    Icons.check_rounded,
                    size: 18,
                    color: AppColors.success,
                  )
                : Text(
                    name.isEmpty ? '?' : name[0].toUpperCase(),
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                    ),
                  ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: done ? AppColors.textSecondary : AppColors.textPrimary,
              ),
            ),
          ),
          IconButton(
            tooltip: 'Conversar com $name',
            visualDensity: VisualDensity.compact,
            onPressed: onChat,
            icon: const Icon(
              Icons.chat_bubble_outline_rounded,
              size: 19,
              color: AppColors.textPrimary,
            ),
          ),
          if (!done && onNoShow != null)
            TextButton(
              key: ValueKey('trip_no_show_${passenger.userId}'),
              onPressed: isBusy ? null : onNoShow,
              style: TextButton.styleFrom(
                foregroundColor: AppColors.error,
                visualDensity: VisualDensity.compact,
              ),
              child: const Text(
                'Não apareceu',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          if (!done && actionLabel != null)
            TextButton(
              key: ValueKey('trip_confirm_${passenger.userId}'),
              onPressed: isBusy
                  ? null
                  : () {
                      HapticFeedback.mediumImpact();
                      onAction();
                    },
              style: TextButton.styleFrom(
                foregroundColor: AppColors.success,
                visualDensity: VisualDensity.compact,
              ),
              child: Text(
                actionLabel!,
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
        ],
      ),
    );
  }
}
