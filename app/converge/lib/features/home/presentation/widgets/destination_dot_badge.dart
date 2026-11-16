// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Marca o destino: o inverso da partida (bolinha branca, borda e centro pretos),
/// igual ao marcador de destino no mapa.
class DestinationDotBadge extends StatelessWidget {
  final double size;

  const DestinationDotBadge({super.key, this.size = 16});

  @override
  Widget build(BuildContext context) {
    // SizedBox fixo: em espaços maiores a bolinha não estica
    return SizedBox.square(
      dimension: size,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: AppColors.cardBackground,
          shape: BoxShape.circle,
          border: Border.all(color: AppColors.primary, width: size * 0.18),
          boxShadow: [
            BoxShadow(
              color: AppColors.primary.withValues(alpha: 0.18),
              blurRadius: 3,
              offset: const Offset(0, 1),
            ),
          ],
        ),
        child: Center(
          child: SizedBox.square(
            dimension: size * 0.3,
            child: const DecoratedBox(
              decoration: BoxDecoration(
                color: AppColors.primary,
                shape: BoxShape.circle,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
