// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import 'indicator_format.dart';

/// Destaque da aba Ambiental: CO₂ evitado e a equivalência em árvores.
class Co2HighlightCard extends StatelessWidget {
  final double co2AvoidedKg;
  final double equivalentTrees;

  const Co2HighlightCard({
    super.key,
    required this.co2AvoidedKg,
    required this.equivalentTrees,
  });

  @override
  Widget build(BuildContext context) {
    final trees = formatIndicatorDecimal(equivalentTrees);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 22),
      decoration: BoxDecoration(
        color: AppColors.successBackground,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: AppColors.successBorder, width: 1.4),
      ),
      child: Column(
        children: [
          const Text(
            'CO₂ POTENCIALMENTE EVITADO',
            style: TextStyle(
              fontSize: 11.5,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.6,
              color: AppColors.success,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            '${formatIndicatorDecimal(co2AvoidedKg)} kg',
            key: const ValueKey('co2_avoided_value'),
            style: const TextStyle(
              fontSize: 40,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
              letterSpacing: -1,
            ),
          ),
          const SizedBox(height: 4),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.park_rounded,
                size: 16,
                color: AppColors.success,
              ),
              const SizedBox(width: 6),
              Text(
                equivalentTrees == 1
                    ? '1 árvore em um ano'
                    : '$trees árvores em um ano',
                style: const TextStyle(
                  fontSize: 12.5,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
