// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../data/models/payment_status.dart';
import '../../data/models/ride_history_item.dart';
import '../../data/models/ride_status.dart';
import 'ride_role_tag.dart';

/// Uma viagem do histórico: quando, de onde para onde, o carro, quantas pessoas
/// foram, o valor e se já foi pago.
class RideHistoryTile extends StatelessWidget {
  final RideHistoryItem item;

  const RideHistoryTile({super.key, required this.item});

  static String _twoDigits(int value) => value.toString().padLeft(2, '0');

  static String _when(DateTime? date) {
    if (date == null) return 'Data a definir';
    return '${_twoDigits(date.day)}/${_twoDigits(date.month)} · '
        '${_twoDigits(date.hour)}:${_twoDigits(date.minute)}';
  }

  // Nome curto do lugar: o bairro, ou o começo do endereço
  static String _short(RoutePlaceModel place) {
    final neighborhood = place.neighborhood?.trim() ?? '';
    if (neighborhood.isNotEmpty) return neighborhood;
    return place.address.split(',').first.trim();
  }

  String _amount() {
    final paid = item.paidAmount;
    final estimated = item.estimatedAmount;
    if (paid != null) {
      return item.isDriver
          ? 'recebeu ${CurrencyFormatter.format(paid)}'
          : 'pagou ${CurrencyFormatter.format(paid)}';
    }
    if (estimated == null) return 'sem valor';
    final pending =
        !item.isDriver && item.paymentStatus == PaymentStatus.pending;
    return '${CurrencyFormatter.format(estimated)}${pending ? ' · a pagar' : ''}';
  }

  @override
  Widget build(BuildContext context) {
    final cancelled = item.status == RideStatus.cancelled;
    final details = [
      if (item.vehicleModel?.isNotEmpty ?? false) item.vehicleModel!,
      '${item.people} pess.',
      _amount(),
    ].join(' · ');

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 6,
            children: [
              Text(
                _when(item.departureDateTime),
                style: const TextStyle(
                  fontSize: 12.5,
                  color: AppColors.textSecondary,
                ),
              ),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  RideRoleTag(isDriver: item.isDriver),
                  const SizedBox(width: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: cancelled
                          ? AppColors.errorBackground
                          : AppColors.successBackground,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      item.status.label,
                      style: TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w700,
                        color: cancelled
                            ? AppColors.errorText
                            : AppColors.success,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            '${_short(item.origin)} → ${_short(item.destination)}',
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            details,
            style: const TextStyle(
              fontSize: 12.5,
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
