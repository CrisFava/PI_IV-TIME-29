// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../config/styles.dart';

/// "Mais informações ›" no canto de um card que abre a folha de detalhes.
class MoreInfoHint extends StatelessWidget {
  const MoreInfoHint({super.key});

  @override
  Widget build(BuildContext context) {
    return const Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'Mais informações',
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w700,
            color: AppColors.textPrimary,
          ),
        ),
        Icon(
          Icons.chevron_right_rounded,
          size: 20,
          color: AppColors.textPrimary,
        ),
      ],
    );
  }
}
