// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';

/// "Repetir toda semana" da carona oferecida. Desmarcado (padrão), a carona
/// vale só no dia escolhido; marcado, se repete no mesmo dia e horário.
class RepeatWeeklyCheckbox extends StatelessWidget {
  final bool value;
  final ValueChanged<bool> onChanged;

  const RepeatWeeklyCheckbox({
    super.key,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      key: const ValueKey('repeat_weekly_checkbox'),
      borderRadius: BorderRadius.circular(12),
      onTap: () {
        HapticFeedback.lightImpact();
        onChanged(!value);
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            Checkbox(
              value: value,
              onChanged: (checked) {
                HapticFeedback.lightImpact();
                onChanged(checked ?? false);
              },
              activeColor: AppColors.primary,
              checkColor: AppColors.textLight,
              side: const BorderSide(color: AppColors.borderMedium, width: 1.5),
            ),
            const Expanded(
              child: Text(
                'Repetir toda semana, no mesmo dia e horário',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
