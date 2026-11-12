// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/match_model.dart';
import 'match_date_label.dart';

/// Painel do passageiro enquanto o motorista não responde ao pedido de vaga.
class MatchWaitingCard extends StatelessWidget {
  final MatchModel? match;
  final bool isCancelling;
  final VoidCallback onCancel;

  const MatchWaitingCard({
    super.key,
    required this.match,
    required this.onCancel,
    this.isCancelling = false,
  });

  @override
  Widget build(BuildContext context) {
    final date = match?.nextRideDate;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const SizedBox(
                width: 22,
                height: 22,
                child: CircularProgressIndicator(
                  strokeWidth: 2.5,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Aguardando o motorista...',
                      style: TextStyle(
                        fontSize: 15.5,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      match == null
                          ? 'Pedido de vaga enviado'
                          : 'Pedido enviado para ${match!.otherUserName}',
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
          if (date != null) ...[
            const SizedBox(height: 12),
            Text(
              'Viagem de ${matchDateLabel(date)}, saída às ${match!.departureTime}. '
              'Assim que o motorista aceitar, o chat é liberado.',
              style: const TextStyle(
                fontSize: 12.5,
                color: AppColors.textMedium,
              ),
            ),
          ],
          const SizedBox(height: 16),
          OutlinedButton(
            onPressed: isCancelling
                ? null
                : () {
                    HapticFeedback.lightImpact();
                    onCancel();
                  },
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.error,
              side: const BorderSide(color: AppColors.errorBorder),
              padding: const EdgeInsets.symmetric(vertical: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
              ),
            ),
            child: isCancelling
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.error,
                    ),
                  )
                : const Text(
                    'Cancelar pedido',
                    style: TextStyle(
                      fontSize: 13.5,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
