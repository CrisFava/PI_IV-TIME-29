// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';

/// Linha de opções em chips (tipo do veículo, combustível...).
class VehicleChoiceChips<T> extends StatelessWidget {
  final String title;
  final List<T> options;
  final T selected;
  final String Function(T option) labelOf;
  final ValueChanged<T> onSelected;
  final bool enabled;

  const VehicleChoiceChips({
    super.key,
    required this.title,
    required this.options,
    required this.selected,
    required this.labelOf,
    required this.onSelected,
    this.enabled = true,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final option in options)
              ChoiceChip(
                label: Text(labelOf(option)),
                selected: option == selected,
                selectedColor: AppColors.primary,
                backgroundColor: AppColors.chipBackground,
                labelStyle: TextStyle(
                  color: option == selected
                      ? AppColors.dockItemActive
                      : AppColors.textPrimary,
                  fontWeight: option == selected
                      ? FontWeight.w700
                      : FontWeight.w500,
                  fontSize: 12.5,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                  side: BorderSide(
                    color: option == selected
                        ? AppColors.primary
                        : AppColors.chipBorder,
                  ),
                ),
                showCheckmark: false,
                onSelected: enabled
                    ? (isSelected) {
                        if (!isSelected) return;
                        HapticFeedback.selectionClick();
                        onSelected(option);
                      }
                    : null,
              ),
          ],
        ),
      ],
    );
  }
}
