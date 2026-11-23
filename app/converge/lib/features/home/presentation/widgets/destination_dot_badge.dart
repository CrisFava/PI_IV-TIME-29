// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Marca o destino com a bandeira de chegada, igual ao marcador de destino no
/// mapa (o mesmo padrão em todas as telas com percurso).
class DestinationDotBadge extends StatelessWidget {
  final double size;

  const DestinationDotBadge({super.key, this.size = 16});

  @override
  Widget build(BuildContext context) {
    // SizedBox fixo: em espaços maiores o ícone não estica
    return SizedBox.square(
      dimension: size,
      child: DecoratedBox(
        decoration: const BoxDecoration(
          color: AppColors.primary,
          shape: BoxShape.circle,
        ),
        child: Icon(
          Icons.flag_rounded,
          size: size * 0.62,
          color: AppColors.textLight,
        ),
      ),
    );
  }
}
