// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../data/models/driver_offer.dart';
import 'driver_ride_card.dart';
import 'offer_schedule_label.dart';

/// Uma carona que o motorista oferece: quando sai, de onde para onde e as viagens
/// dela (com os passageiros e "Iniciar trajeto"). Dá para cancelar a oferta.
class DriverOfferCard extends StatelessWidget {
  final DriverOffer offer;
  final Set<String> busyRideIds;
  final bool isCancelling;
  final ValueChanged<String> onStartRide;
  final ValueChanged<String> onCompleteRide;
  final void Function(String rideId, String passengerId) onOpenChat;
  final VoidCallback onCancelOffer;

  const DriverOfferCard({
    super.key,
    required this.offer,
    required this.onStartRide,
    required this.onCompleteRide,
    required this.onOpenChat,
    required this.onCancelOffer,
    this.busyRideIds = const {},
    this.isCancelling = false,
  });

  @override
  Widget build(BuildContext context) {
    final route = offer.route;

    return Container(
      padding: const EdgeInsets.fromLTRB(14, 10, 6, 14),
      decoration: BoxDecoration(
        color: AppColors.cardSecondary,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (route != null) ...[
            Row(
              children: [
                const Icon(
                  Icons.event_repeat_rounded,
                  size: 18,
                  color: AppColors.textPrimary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    offerScheduleLabel(route.weekDays, route.departureTime),
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ),
                isCancelling
                    ? const Padding(
                        padding: EdgeInsets.all(12),
                        child: SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: AppColors.error,
                          ),
                        ),
                      )
                    : IconButton(
                        key: ValueKey('cancel_offer_${route.id}'),
                        tooltip: 'Cancelar esta carona',
                        visualDensity: VisualDensity.compact,
                        onPressed: () {
                          HapticFeedback.lightImpact();
                          onCancelOffer();
                        },
                        icon: const Icon(
                          Icons.delete_outline_rounded,
                          size: 20,
                          color: AppColors.error,
                        ),
                      ),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  RidePlaceLine(
                    icon: Icons.trip_origin_rounded,
                    text: route.origin.address,
                  ),
                  const SizedBox(height: 4),
                  RidePlaceLine(
                    icon: Icons.flag_rounded,
                    text: route.destination.address,
                  ),
                ],
              ),
            ),
          ],
          if (offer.rides.isEmpty)
            const Padding(
              padding: EdgeInsets.only(top: 10, right: 8),
              child: Text(
                'Ainda sem pedidos de vaga.',
                style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
              ),
            )
          else
            for (final ride in offer.rides)
              Padding(
                padding: EdgeInsets.only(top: route == null ? 0 : 10, right: 8),
                child: DriverRideCard(
                  ride: ride,
                  isBusy: busyRideIds.contains(ride.id),
                  onStart: () => onStartRide(ride.id),
                  onComplete: () => onCompleteRide(ride.id),
                  onOpenChat: (passengerId) => onOpenChat(ride.id, passengerId),
                ),
              ),
        ],
      ),
    );
  }
}
