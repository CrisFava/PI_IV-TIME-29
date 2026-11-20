// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/more_info_hint.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import 'match_date_label.dart';

/// Uma viagem em que o usuário vai como passageiro, em "Minhas caronas": quando
/// sai, de onde para onde e quem dirige. Tocar abre os detalhes (com "Sair da viagem").
class PassengerRideCard extends StatelessWidget {
  final RideModel ride;
  final String viewerId;
  final VoidCallback onOpenDetails;

  const PassengerRideCard({
    super.key,
    required this.ride,
    required this.viewerId,
    required this.onOpenDetails,
  });

  @override
  Widget build(BuildContext context) {
    final departure = ride.departureDateTime;
    final own = ride.passengerOf(viewerId);
    final inProgress = ride.status == RideStatus.inProgress;
    final vehicle = [
      if (ride.vehicleModel?.isNotEmpty ?? false) ride.vehicleModel!,
      if (ride.vehicleLicensePlate?.isNotEmpty ?? false)
        ride.vehicleLicensePlate!,
    ].join(' · ');

    return Material(
      color: inProgress ? AppColors.successBackground : AppColors.cardSecondary,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        key: ValueKey('passenger_ride_card_${ride.id}'),
        borderRadius: BorderRadius.circular(20),
        onTap: () {
          HapticFeedback.lightImpact();
          onOpenDetails();
        },
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: inProgress
                  ? AppColors.successBorder
                  : AppColors.borderSubtle,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const Icon(
                    Icons.event_rounded,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
                  const SizedBox(width: 8),
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
                ],
              ),
              const SizedBox(height: 8),
              RidePlaceLine(
                icon: Icons.trip_origin_rounded,
                text: (own?.boardingLocation ?? ride.origin).address,
              ),
              const SizedBox(height: 4),
              RidePlaceLine(
                icon: Icons.flag_rounded,
                text: (own?.dropOffLocation ?? ride.destination).address,
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      inProgress
                          ? 'Viagem em andamento'
                          : [
                              ride.driverName,
                              if (vehicle.isNotEmpty) vehicle,
                            ].join(' · '),
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        color: inProgress
                            ? AppColors.success
                            : AppColors.textSecondary,
                      ),
                    ),
                  ),
                  const MoreInfoHint(),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
