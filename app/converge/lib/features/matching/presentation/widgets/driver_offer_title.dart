// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../../route/data/models/route_recurrence.dart';
import '../../data/models/driver_offer.dart';
import 'match_date_label.dart';
import 'offer_schedule_label.dart';

/// Quando a carona sai, com o ícone de "toda semana" ou de "um dia só".
class DriverOfferTitle extends StatelessWidget {
  final DriverOffer offer;
  final double fontSize;

  const DriverOfferTitle({super.key, required this.offer, this.fontSize = 14});

  @override
  Widget build(BuildContext context) {
    final route = offer.route;
    final departure = offer.rides.isEmpty
        ? null
        : offer.rides.first.departureDateTime;
    final weekly = route?.recurrence == RouteRecurrence.weekly;

    final label = route != null
        ? offerScheduleLabel(
            route.weekDays,
            route.departureTime,
            date: route.date,
          )
        : (departure == null
              ? 'Data a definir'
              : matchDateTimeLabel(departure));

    return Row(
      children: [
        Icon(
          weekly ? Icons.event_repeat_rounded : Icons.event_rounded,
          size: fontSize + 4,
          color: AppColors.textPrimary,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            label,
            style: TextStyle(
              fontSize: fontSize,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
        ),
      ],
    );
  }
}
