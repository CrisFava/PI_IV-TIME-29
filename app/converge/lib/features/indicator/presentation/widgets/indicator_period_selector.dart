// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/indicator_period.dart';

/// Escolha do período dos indicadores: semana, mês, semestre ou total.
class IndicatorPeriodSelector extends StatelessWidget {
  final IndicatorPeriod selected;
  final ValueChanged<IndicatorPeriod> onChanged;

  const IndicatorPeriodSelector({
    super.key,
    required this.selected,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final period in IndicatorPeriod.values)
          ChoiceChip(
            key: ValueKey('period_${period.apiValue}'),
            label: Text(period.label),
            selected: period == selected,
            showCheckmark: false,
            selectedColor: AppColors.primary,
            backgroundColor: AppColors.chipBackground,
            labelStyle: TextStyle(
              fontSize: 12.5,
              fontWeight: period == selected
                  ? FontWeight.w700
                  : FontWeight.w500,
              color: period == selected
                  ? AppColors.dockItemActive
                  : AppColors.textPrimary,
            ),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
              side: BorderSide(
                color: period == selected
                    ? AppColors.primary
                    : AppColors.chipBorder,
              ),
            ),
            onSelected: (isSelected) {
              if (!isSelected) return;
              HapticFeedback.selectionClick();
              onChanged(period);
            },
          ),
      ],
    );
  }
}
