// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Bolinha preta opaca que marca a partida, igual ao pino do mapa.
class OriginDotBadge extends StatelessWidget {
  final double size;

  const OriginDotBadge({super.key, this.size = 16});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: AppColors.primary,
        shape: BoxShape.circle,
        border: Border.all(color: AppColors.cardBackground, width: 1.5),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: 0.18),
            blurRadius: 3,
            offset: const Offset(0, 1),
          ),
        ],
      ),
    );
  }
}
