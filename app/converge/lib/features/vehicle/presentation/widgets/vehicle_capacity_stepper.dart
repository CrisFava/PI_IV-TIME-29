// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';

/// Escolha da quantidade de lugares do veículo (contando o motorista).
class VehicleCapacityStepper extends StatelessWidget {
  final int value;
  final int max;
  final ValueChanged<int> onChanged;
  final String? errorText;
  final bool enabled;

  const VehicleCapacityStepper({
    super.key,
    required this.value,
    required this.max,
    required this.onChanged,
    this.errorText,
    this.enabled = true,
  });

  static const int min = 2;

  @override
  Widget build(BuildContext context) {
    final seats = value - 1;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
          decoration: BoxDecoration(
            color: AppColors.inputBackground,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: errorText == null
                  ? AppColors.borderSubtle
                  : AppColors.errorBorder,
            ),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.event_seat_rounded,
                size: 20,
                color: AppColors.textSecondary,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '$value lugares',
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      'Motorista + $seats ${seats == 1 ? 'passageiro' : 'passageiros'}',
                      style: const TextStyle(
                        fontSize: 11.5,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                tooltip: 'Menos lugares',
                onPressed: enabled && value > min
                    ? () {
                        HapticFeedback.selectionClick();
                        onChanged(value - 1);
                      }
                    : null,
                icon: const Icon(Icons.remove_circle_outline_rounded),
                color: AppColors.textPrimary,
              ),
              IconButton(
                tooltip: 'Mais lugares',
                onPressed: enabled && value < max
                    ? () {
                        HapticFeedback.selectionClick();
                        onChanged(value + 1);
                      }
                    : null,
                icon: const Icon(Icons.add_circle_outline_rounded),
                color: AppColors.textPrimary,
              ),
            ],
          ),
        ),
        if (errorText != null)
          Padding(
            padding: const EdgeInsets.only(left: 12, top: 6),
            child: Text(
              errorText!,
              style: const TextStyle(fontSize: 12, color: AppColors.errorText),
            ),
          ),
      ],
    );
  }
}
