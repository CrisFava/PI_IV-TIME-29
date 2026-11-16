// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// A placa do carro em destaque, com borda, como uma placa de verdade.
class LicensePlateBadge extends StatelessWidget {
  final String plate;

  const LicensePlateBadge({super.key, required this.plate});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.textPrimary, width: 1.6),
      ),
      child: Text(
        plate,
        style: const TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w800,
          letterSpacing: 1.4,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}
