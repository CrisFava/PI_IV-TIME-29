// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Linha com um ícone e um endereço (origem ou destino da viagem).
class RidePlaceLine extends StatelessWidget {
  final IconData icon;
  final String text;

  /// Linhas do endereço antes de cortar com "..."; nulo mostra o endereço inteiro.
  final int? maxLines;

  /// Texto maior e escuro, para as folhas de detalhes.
  final bool emphasized;

  const RidePlaceLine({
    super.key,
    required this.icon,
    required this.text,
    this.maxLines = 1,
    this.emphasized = false,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          icon,
          size: emphasized ? 17 : 15,
          color: emphasized ? AppColors.textPrimary : AppColors.textSecondary,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            maxLines: maxLines,
            overflow: maxLines == null ? null : TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: emphasized ? 14 : 12.5,
              fontWeight: emphasized ? FontWeight.w600 : FontWeight.w400,
              color: emphasized
                  ? AppColors.textPrimary
                  : AppColors.textSecondary,
            ),
          ),
        ),
      ],
    );
  }
}
