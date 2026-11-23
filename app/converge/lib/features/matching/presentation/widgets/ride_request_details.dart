// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_section.dart';
import '../../../../core/widgets/details_summary_card.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../ride/data/models/ride_preview_model.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../../ride/presentation/widgets/ride_route_overview.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/ride_request_actions_controller.dart';
import '../../state/ride_request_preview_provider.dart';

/// Detalhes de um pedido de vaga: de onde o passageiro sai, para onde vai, a hora que
/// ele quer sair, o desvio que ele acrescenta e como a viagem ficaria com ele, nos
/// mesmos passos do trajeto completo (os dele em destaque) e com a nova chegada.
class RideRequestDetails extends ConsumerWidget {
  final DriverRideRequest request;
  final VoidCallback onAccept;
  final VoidCallback onReject;
  final VoidCallback onOpenChat;

  const RideRequestDetails({
    super.key,
    required this.request,
    required this.onAccept,
    required this.onReject,
    required this.onOpenChat,
  });

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';

  /// O desvio em tempo, que é o principal: o Google sempre escolhe o caminho mais
  /// rápido, então a distância só aparece de apoio, embaixo.
  static String _detourText(RidePreviewModel preview) {
    final minutes = preview.extraMinutes;
    if (minutes == null) return 'Não foi possível calcular o tempo a mais.';
    if (minutes <= 0) return 'Ele está no seu caminho: sem tempo a mais.';
    return 'Desvio: +$minutes min no seu tempo de viagem.';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final passenger = request.passenger;
    final preview = ref.watch(
      rideRequestPreviewProvider((
        rideId: request.rideId,
        passengerId: request.passengerId,
      )),
    );
    final busy = ref.watch(rideRequestActionsProvider).contains(request.key);
    final departure = request.ride.departureDateTime;
    final boarding = passenger.boardingLocation;
    final dropOff = passenger.dropOffLocation;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          passenger.displayName,
          style: const TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
        // O desvio que ele acrescenta (o mapa, embaixo, mostra em preto)
        if (preview.value case final value?)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            // O tempo a mais é o principal; a distância vem de apoio, menor
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _detourText(value),
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
                if (value.withPassengerDistanceKm > 0)
                  Text(
                    'Com ele, a viagem fica com '
                    '${value.withPassengerDistanceKm.toStringAsFixed(1).replaceAll('.', ',')} km.',
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
              ],
            ),
          ),
        DetailsSection(
          title: 'Embarque',
          child: RidePlaceLine(
            icon: Icons.person_pin_circle_rounded,
            text: boarding?.address ?? 'Não informado',
            maxLines: null,
            emphasized: true,
          ),
        ),
        DetailsSection(
          title: 'Desembarque',
          child: RidePlaceLine(
            icon: Icons.flag_rounded,
            text: dropOff?.address ?? 'Não informado',
            maxLines: null,
            emphasized: true,
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(top: 12),
          child: DetailsSummaryCard(
            rows: [
              (
                icon: Icons.schedule_rounded,
                label: 'Quer sair às',
                value: passenger.requestedDepartureTime ?? '—',
              ),
              (
                icon: Icons.directions_car_rounded,
                label: 'Sua saída',
                value: departure == null ? '—' : _hhmm(departure),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                key: const ValueKey('request_details_reject'),
                onPressed: busy
                    ? null
                    : () {
                        HapticFeedback.lightImpact();
                        onReject();
                      },
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textPrimary,
                  side: const BorderSide(color: AppColors.borderMedium),
                  padding: const EdgeInsets.symmetric(vertical: 13),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: const Text(
                  'Recusar',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: FilledButton(
                key: const ValueKey('request_details_accept'),
                onPressed: busy
                    ? null
                    : () {
                        HapticFeedback.mediumImpact();
                        onAccept();
                      },
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.textLight,
                  padding: const EdgeInsets.symmetric(vertical: 13),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: busy
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: AppColors.textLight,
                        ),
                      )
                    : const Text(
                        'Aceitar',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
              ),
            ),
          ],
        ),
        TextButton.icon(
          onPressed: () {
            HapticFeedback.lightImpact();
            onOpenChat();
          },
          style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
          icon: const Icon(Icons.chat_bubble_outline_rounded, size: 18),
          label: Text(
            'Conversar com ${passenger.displayName.split(' ').first}',
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Como fica a viagem',
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: 6),
        // A viagem como ficaria, nos mesmos passos do trajeto completo: os passos
        // do passageiro novo em destaque e o horário em que o carro chega a cada um
        switch (preview) {
          AsyncData(:final value) => RideRouteOverview.ofPreview(
            request.ride,
            value,
            passengerId: request.passengerId,
            passengerName: passenger.displayName,
          ),
          AsyncError(:final error) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Text(
              authErrorMessage(error),
              style: const TextStyle(
                fontSize: 13,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          _ => const Padding(
            padding: EdgeInsets.all(24),
            child: Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
          ),
        },
      ],
    );
  }
}
