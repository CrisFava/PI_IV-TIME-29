// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../data/models/match_model.dart';
import 'match_date_label.dart';
import 'match_info_chip.dart';

/// Um motorista compatível na lista do passageiro, com o botão de pedir a vaga.
class MatchDriverCard extends StatelessWidget {
  final MatchModel match;

  /// Este cartão está enviando o pedido.
  final bool isRequesting;

  /// Outro pedido está em andamento: o botão fica desabilitado.
  final bool isDisabled;
  final VoidCallback onRequest;

  const MatchDriverCard({
    super.key,
    required this.match,
    required this.onRequest,
    this.isRequesting = false,
    this.isDisabled = false,
  });

  @override
  Widget build(BuildContext context) {
    final rating = match.otherUserAverageRating;
    final cost = match.estimatedCostPerPassenger;
    final maxAmount = match.maxAmount;
    final canTap = !isDisabled && !isRequesting && match.canRequestSeat;

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
          Row(
            children: [
              const CircleAvatar(
                radius: 22,
                backgroundColor: AppColors.neutralLight,
                child: Icon(Icons.person_rounded, color: AppColors.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      rating == null
                          ? '${match.otherUserName} • novo'
                          : '${match.otherUserName} • ★ ${rating.toStringAsFixed(1)}',
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      match.vehicle?.description ?? 'Veículo não informado',
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: AppColors.textSecondary,
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
              MatchInfoChip(
                icon: Icons.schedule_rounded,
                label: match.nextRideDate == null
                    ? 'Sai às ${match.departureTime}'
                    : '${matchDateLabel(match.nextRideDate!)} às ${match.departureTime}',
              ),
              if (match.availableSeats != null)
                MatchInfoChip(
                  icon: Icons.event_seat_rounded,
                  label: match.availableSeats == 1
                      ? '1 vaga'
                      : '${match.availableSeats} vagas',
                ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      cost == null
                          ? 'Valor a combinar'
                          : CurrencyFormatter.format(cost),
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      maxAmount == null
                          ? 'estimativa por passageiro'
                          : 'até ${CurrencyFormatter.format(maxAmount)} se alguém desistir',
                      style: const TextStyle(
                        fontSize: 11,
                        color: AppColors.textMuted,
                      ),
                    ),
                  ],
                ),
              ),
              ElevatedButton(
                onPressed: canTap
                    ? () {
                        HapticFeedback.mediumImpact();
                        onRequest();
                      }
                    : null,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.textLight,
                  disabledBackgroundColor: AppColors.borderMedium,
                  disabledForegroundColor: AppColors.textLight,
                  elevation: 0,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 18,
                    vertical: 10,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child: isRequesting
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: AppColors.textLight,
                        ),
                      )
                    : const Text(
                        'Pedir vaga',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
