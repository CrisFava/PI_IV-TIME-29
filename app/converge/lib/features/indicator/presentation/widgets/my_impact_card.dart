// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/indicator_period.dart';
import '../../state/indicator_providers.dart';
import 'indicator_format.dart';

/// Card "Meu impacto" do Perfil: o CO₂ evitado e as caronas desde o começo.
/// Tocar abre os Indicadores.
class MyImpactCard extends ConsumerWidget {
  final VoidCallback onOpen;

  const MyImpactCard({super.key, required this.onOpen});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final total = ref
        .watch(myIndicatorsProvider(IndicatorPeriod.allTime))
        .value;

    return Material(
      color: AppColors.successBackground,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        key: const ValueKey('my_impact_card'),
        borderRadius: BorderRadius.circular(22),
        onTap: () {
          HapticFeedback.lightImpact();
          onOpen();
        },
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: AppColors.successBorder),
          ),
          child: Row(
            children: [
              const Icon(Icons.eco_rounded, size: 28, color: AppColors.success),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Meu impacto',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      total == null
                          ? 'Veja seus indicadores de mobilidade e ambientais'
                          : '${formatIndicatorDecimal(total.co2AvoidedKg)} kg de CO₂ evitados em '
                                '${formatIndicatorInt(total.totalRides)} '
                                '${total.totalRides == 1 ? 'carona' : 'caronas'}',
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.chevron_right_rounded,
                color: AppColors.textMuted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
