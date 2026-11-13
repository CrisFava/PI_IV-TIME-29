// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/ride_model.dart';
import '../../data/models/ride_status.dart';
import 'ride_place_line.dart';
import 'ride_role_tag.dart';

/// Uma viagem do histórico, com o papel do usuário nela (motorista ou passageiro).
class RideHistoryTile extends StatelessWidget {
  final RideModel ride;
  final bool isDriver;

  const RideHistoryTile({
    super.key,
    required this.ride,
    required this.isDriver,
  });

  static String _twoDigits(int value) => value.toString().padLeft(2, '0');

  static String _dateLabel(DateTime? date) {
    if (date == null) return 'Data a definir';
    return '${_twoDigits(date.day)}/${_twoDigits(date.month)}/${date.year} '
        'às ${_twoDigits(date.hour)}:${_twoDigits(date.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    final (statusBackground, statusColor) = switch (ride.status) {
      RideStatus.completed => (AppColors.successBackground, AppColors.success),
      RideStatus.cancelled => (AppColors.errorBackground, AppColors.errorText),
      _ => (AppColors.neutralLight, AppColors.textDark),
    };
    final vehicle = [
      if (ride.vehicleModel?.isNotEmpty ?? false) ride.vehicleModel!,
      if (ride.vehicleLicensePlate?.isNotEmpty ?? false)
        ride.vehicleLicensePlate!,
    ].join(' • ');

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              RideRoleTag(isDriver: isDriver),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: statusBackground,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  ride.status.label,
                  style: TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w700,
                    color: statusColor,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            _dateLabel(ride.departureDateTime),
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
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
          if (vehicle.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              vehicle,
              style: const TextStyle(
                fontSize: 11.5,
                color: AppColors.textMuted,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
