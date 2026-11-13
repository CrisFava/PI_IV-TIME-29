// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../data/models/driver_ride_request.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import 'match_date_label.dart';
import 'match_info_chip.dart';

/// Um passageiro numa viagem do motorista: pedido para aceitar ou recusar,
/// ou passageiro já aceito, com o atalho para o chat.
class RideRequestCard extends StatelessWidget {
  final DriverRideRequest request;
  final bool isBusy;
  final VoidCallback onAccept;
  final VoidCallback onReject;
  final VoidCallback onOpenChat;

  const RideRequestCard({
    super.key,
    required this.request,
    required this.onAccept,
    required this.onReject,
    required this.onOpenChat,
    this.isBusy = false,
  });

  @override
  Widget build(BuildContext context) {
    final ride = request.ride;
    final departure = ride.departureDateTime;
    final pending = request.passenger.status == ParticipantStatus.requested;
    final isFull = ride.availableSeats <= 0;
    final boarding = request.passenger.boardingLocation?.address ?? '';

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: pending ? AppColors.borderCardPressed : AppColors.borderSubtle,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const CircleAvatar(
                radius: 20,
                backgroundColor: AppColors.neutralLight,
                child: Icon(Icons.person_rounded, color: AppColors.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      request.passenger.displayName,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      pending ? 'Pediu uma vaga' : 'Vaga confirmada',
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        color: pending
                            ? AppColors.textSecondary
                            : AppColors.success,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              if (departure != null)
                MatchInfoChip(
                  icon: Icons.event_rounded,
                  label: matchDateTimeLabel(departure),
                ),
              MatchInfoChip(
                icon: Icons.event_seat_rounded,
                label: isFull
                    ? 'Carona lotada (${ride.totalSeats}/${ride.totalSeats})'
                    : '${ride.availableSeats}/${ride.totalSeats} vagas livres',
                alert: isFull,
              ),
            ],
          ),
          if (boarding.isNotEmpty) ...[
            const SizedBox(height: 8),
            RidePlaceLine(
              icon: Icons.trip_origin_rounded,
              text: 'Embarca em $boarding',
            ),
          ],
          const SizedBox(height: 12),
          if (pending)
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: isBusy
                        ? null
                        : () {
                            HapticFeedback.lightImpact();
                            onReject();
                          },
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.error,
                      side: const BorderSide(color: AppColors.errorBorder),
                      padding: const EdgeInsets.symmetric(vertical: 11),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
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
                  child: ElevatedButton(
                    // Sem vaga livre, só dá para recusar
                    onPressed: isBusy || isFull
                        ? null
                        : () {
                            HapticFeedback.mediumImpact();
                            onAccept();
                          },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: AppColors.textLight,
                      disabledBackgroundColor: AppColors.borderMedium,
                      disabledForegroundColor: AppColors.textLight,
                      elevation: 0,
                      padding: const EdgeInsets.symmetric(vertical: 11),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: isBusy
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: AppColors.textLight,
                            ),
                          )
                        : Text(
                            isFull ? 'Lotada' : 'Aceitar',
                            style: const TextStyle(fontWeight: FontWeight.w700),
                          ),
                  ),
                ),
              ],
            )
          else
            ElevatedButton.icon(
              onPressed: () {
                HapticFeedback.lightImpact();
                onOpenChat();
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: AppColors.textLight,
                elevation: 0,
                padding: const EdgeInsets.symmetric(vertical: 11),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              icon: const Icon(Icons.chat_bubble_rounded, size: 18),
              label: const Text(
                'Abrir chat',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
        ],
      ),
    );
  }
}
