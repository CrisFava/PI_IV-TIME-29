// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/indicator_period.dart';
import '../../state/indicator_providers.dart';
import 'indicator_format.dart';

/// Impacto de toda a plataforma no período: caronas e pessoas que usaram o Converge.
class PlatformImpactCard extends ConsumerWidget {
  final IndicatorPeriod period;

  const PlatformImpactCard({super.key, required this.period});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final platform = ref.watch(platformIndicatorsProvider(period));

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardSecondary,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.groups_rounded,
            size: 26,
            color: AppColors.textPrimary,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: switch (platform) {
              AsyncData(:final value) => Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Em todo o Converge',
                    style: TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '${formatIndicatorInt(value.totalRides)} caronas • '
                    '${formatIndicatorInt(value.activeUsers)} pessoas',
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
              AsyncError() => const Text(
                'Não foi possível carregar o total da plataforma.',
                style: TextStyle(
                  fontSize: 12.5,
                  color: AppColors.textSecondary,
                ),
              ),
              _ => const LinearProgressIndicator(
                color: AppColors.primary,
                backgroundColor: AppColors.progressTrack,
              ),
            },
          ),
        ],
      ),
    );
  }
}
