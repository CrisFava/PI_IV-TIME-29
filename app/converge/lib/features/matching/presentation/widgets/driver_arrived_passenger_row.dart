// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../ride/data/models/payment_status.dart';
import '../../../ride/data/models/ride_participant_model.dart';

/// Passageiro que já desceu, com a chegada confirmada: o motorista vê se ele já pagou.
///
/// O texto da direita resume a cobrança: "Pagou", "Sem cobrança", "Cobrado R$ X" ou só "Desceu"
/// enquanto o valor ainda não chegou. Pago fica em verde.
class DriverArrivedPassengerRow extends StatelessWidget {
  /// Passageiro com a viagem concluída (status COMPLETED).
  final RideParticipantModel passenger;

  const DriverArrivedPassengerRow({super.key, required this.passenger});

  @override
  Widget build(BuildContext context) {
    final paid = passenger.paymentStatus == PaymentStatus.paid;
    final amount = passenger.finalAmount;
    final status = switch (passenger.paymentStatus) {
      PaymentStatus.paid => 'Pagou',
      PaymentStatus.notApplicable => 'Sem cobrança',
      _ when amount != null => 'Cobrado ${CurrencyFormatter.format(amount)}',
      _ => 'Desceu',
    };

    return Row(
      children: [
        const Icon(
          Icons.where_to_vote_rounded,
          size: 16,
          color: AppColors.textMuted,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            passenger.displayName,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: AppColors.textSecondary,
            ),
          ),
        ),
        Text(
          status,
          key: ValueKey('arrived_status_${passenger.userId}'),
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w700,
            color: paid ? AppColors.success : AppColors.textSecondary,
          ),
        ),
      ],
    );
  }
}
