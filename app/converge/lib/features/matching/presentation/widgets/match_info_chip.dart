// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Pequena etiqueta com ícone e texto (horário, desvio, vagas...).
class MatchInfoChip extends StatelessWidget {
  final IconData icon;
  final String label;

  /// Destaca a etiqueta em vermelho (ex.: carona lotada).
  final bool alert;

  const MatchInfoChip({
    super.key,
    required this.icon,
    required this.label,
    this.alert = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: alert ? AppColors.errorBackground : AppColors.chipBackground,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: alert ? AppColors.errorBorder : AppColors.chipBorder,
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            size: 13,
            color: alert ? AppColors.errorText : AppColors.textSecondary,
          ),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 11.5,
              fontWeight: FontWeight.w600,
              color: alert ? AppColors.errorText : AppColors.textDark,
            ),
          ),
        ],
      ),
    );
  }
}
