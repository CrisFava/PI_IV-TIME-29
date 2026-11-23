// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_danger_button.dart';
import '../../../../core/widgets/details_section.dart';
import '../../../../core/widgets/details_sheet.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../state/passenger_ride_actions_controller.dart';
import '../../state/passenger_rides_provider.dart';
import 'match_date_label.dart';
import 'matching_message.dart';
import 'passenger_route_preview.dart';

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
    final pickup = own?.boardingLocation ?? ride.origin;
    final dropOff = own?.dropOffLocation ?? ride.destination;
    final departure = ride.departureDateTime;
    final inProgress = ride.status == RideStatus.inProgress;
    // Chegada ao destino dele: a saída mais o tempo até o embarque e o do trecho dele
    final untilPickup = ride.itineraryMyPickupMinutes;
    final onBoard = ride.itineraryMyRideMinutes;
    final arrival = departure == null || untilPickup == null || onBoard == null
        ? null
        : departure.add(Duration(minutes: untilPickup + onBoard));
    // Paradas de outros passageiros com ele no carro, entre o embarque e o desembarque
    final myPickup = ride.itineraryMyPickupStop;
    final myDropOff = ride.itineraryMyDropOffStop;
    final stopsOnTheWay = myPickup == null || myDropOff == null
        ? null
        : myDropOff - myPickup - 1;
    final vehicleModel = ride.vehicleModel?.trim() ?? '';
    final plate = ride.vehicleLicensePlate?.trim() ?? '';
    final color = ride.vehicleColor?.trim() ?? '';
    // Embaixo do modelo: a cor primeiro (é o que se vê de longe) e a placa
    final carDetails = [
      if (color.isNotEmpty) color,
      if (plate.isNotEmpty) plate,
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
            text: pickup.address,
            maxLines: null,
            emphasized: true,
          ),
        ),
        DetailsSection(
          title: 'Desembarque',
          child: RidePlaceLine(
            icon: Icons.flag_rounded,
            text: dropOff.address,
            maxLines: null,
            emphasized: true,
          ),
        ),
        // Antes de começar: onde o carro pega e deixa, no mapa (durante a
        // viagem, o mapa principal já mostra o caminho)
        if (!inProgress)
          Padding(
            padding: const EdgeInsets.only(top: 10),
            child: OutlinedButton.icon(
              key: const ValueKey('passenger_show_route'),
              onPressed: () => showDetailsSheet<void>(
                context,
                title: 'Seu trajeto',
                builder: (_) => PassengerRoutePreview.ofRide(
                  ride,
                  pickup: pickup,
                  dropOff: dropOff,
                ),
              ),
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.textPrimary,
                side: const BorderSide(color: AppColors.borderMedium),
                padding: const EdgeInsets.symmetric(vertical: 11),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              icon: const Icon(Icons.map_rounded, size: 19),
              label: const Text(
                'Ver no mapa',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ),
        // Os horários numa linha do tempo e, embaixo, o motorista com o carro
        if (departure != null)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: _ScheduleCard(
              departure: departure,
              pickup: untilPickup == null
                  ? null
                  : departure.add(Duration(minutes: untilPickup)),
              arrival: arrival,
              stopsOnTheWay: stopsOnTheWay,
            ),
          ),
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: _DriverCarCard(
            driverName: ride.driverName,
            car: [
              if (vehicleModel.isNotEmpty) vehicleModel,
              if (carDetails.isNotEmpty) carDetails,
            ].join(' · '),
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

String _hhmm(DateTime time) =>
    '${time.hour.toString().padLeft(2, '0')}:'
    '${time.minute.toString().padLeft(2, '0')}';

/// Os horários da viagem para o passageiro, numa linha do tempo: a saída do
/// motorista, quando ele chega até o passageiro e quando o passageiro chega.
class _ScheduleCard extends StatelessWidget {
  final DateTime departure;
  final DateTime? pickup;
  final DateTime? arrival;

  /// Paradas de outros passageiros com ele no carro.
  final int? stopsOnTheWay;

  const _ScheduleCard({
    required this.departure,
    required this.pickup,
    required this.arrival,
    required this.stopsOnTheWay,
  });

  @override
  Widget build(BuildContext context) {
    // Um aviso só, no rodapé: os horários são estimados (e contam as paradas)
    final footnote = switch (stopsOnTheWay) {
      null || <= 0 => 'Horários estimados.',
      1 =>
        'Horários estimados, contando 1 parada de outro passageiro no caminho.',
      final n =>
        'Horários estimados, contando $n paradas de outros passageiros no caminho.',
    };
    final estimated = pickup != null || arrival != null;
    final rows = [
      (
        icon: Icons.schedule_rounded,
        time: _hhmm(departure),
        text: 'Saída do motorista',
      ),
      if (pickup != null)
        (
          icon: Icons.person_pin_circle_rounded,
          time: _hhmm(pickup!),
          text: 'O motorista chega até você',
        ),
      if (arrival != null)
        (
          icon: Icons.flag_rounded,
          time: _hhmm(arrival!),
          text: 'Você chega ao destino',
        ),
    ];
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.neutralLight,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var i = 0; i < rows.length; i++) ...[
            if (i > 0) const SizedBox(height: 10),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(rows[i].icon, size: 18, color: AppColors.textPrimary),
                const SizedBox(width: 10),
                SizedBox(
                  width: 52,
                  child: Text(
                    rows[i].time,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ),
                Expanded(
                  child: Text(
                    rows[i].text,
                    style: const TextStyle(
                      fontSize: 13.5,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textDark,
                    ),
                  ),
                ),
              ],
            ),
          ],
          if (estimated) ...[
            const SizedBox(height: 10),
            Text(
              footnote,
              style: const TextStyle(
                fontSize: 12,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// O motorista e o carro numa linha só: o nome em destaque e, embaixo, o modelo,
/// a cor (o que se vê de longe) e a placa.
class _DriverCarCard extends StatelessWidget {
  final String driverName;
  final String car;

  const _DriverCarCard({required this.driverName, required this.car});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.neutralLight,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 20,
            backgroundColor: AppColors.cardBackground,
            child: Icon(Icons.person_rounded, color: AppColors.primary),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  driverName,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
                if (car.isNotEmpty)
                  Row(
                    children: [
                      const Icon(
                        Icons.directions_car_rounded,
                        size: 15,
                        color: AppColors.textSecondary,
                      ),
                      const SizedBox(width: 5),
                      Expanded(
                        child: Text(
                          car,
                          style: const TextStyle(
                            fontSize: 13,
                            color: AppColors.textDark,
                          ),
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
