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

  static String _hhmm(DateTime time) =>
      '${_twoDigits(time.hour)}:${_twoDigits(time.minute)}';

  static String _dayAndTime(DateTime time) =>
      '${_twoDigits(time.day)}/${_twoDigits(time.month)}/${time.year} · '
      '${_hhmm(time)}';

  /// "19/11/2026 · 09:36 – 10:05": o dia e os horários reais de início e fim.
  /// Sem início real, o horário marcado pode vir depois do fim (a viagem começou antes
  /// da hora): nesse caso mostra só o fim, em vez de uma faixa ao contrário.
  static String _when(RideHistoryItem item) {
    final start = item.startedAt ?? item.departureDateTime;
    final end = item.completedAt;
    if (start == null && end == null) return 'Data a definir';
    if (start == null || (end != null && end.isBefore(start))) {
      return _dayAndTime(end!);
    }
    if (end == null) return _dayAndTime(start);
    return '${_dayAndTime(start)} – ${_hhmm(end)}';
  }

  // Nome curto do lugar: o bairro, ou o começo do endereço
  static String _short(RoutePlaceModel place) {
    final neighborhood = place.neighborhood?.trim() ?? '';
    if (neighborhood.isNotEmpty) return neighborhood;
    return place.address.split(',').first.trim();
  }

  /// O valor sempre vem depois de uma palavra que diz o que ele é: "pagou", "recebeu",
  /// "a pagar", "custo" (do motorista, ainda sem pagamentos) ou "estimado".
  String _amount() {
    final paid = item.paidAmount;
    final estimated = item.estimatedAmount;
    if (paid != null) {
      return item.isDriver
          ? 'recebeu ${CurrencyFormatter.format(paid)}'
          : 'pagou ${CurrencyFormatter.format(paid)}';
    }
    if (estimated == null) return 'sem valor';

    final value = CurrencyFormatter.format(estimated);
    if (item.isDriver) return 'custo $value';
    return item.paymentStatus == PaymentStatus.pending
        ? 'a pagar $value'
        : 'estimado $value';
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
                _when(item),
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
