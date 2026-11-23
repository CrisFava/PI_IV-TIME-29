// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Etiqueta com o papel do usuário na viagem: "Motorista" ou "Passageiro".
class RideRoleTag extends StatelessWidget {
  final bool isDriver;

  const RideRoleTag({super.key, required this.isDriver});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: isDriver ? AppColors.primary : AppColors.chipBackground,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: isDriver ? AppColors.primary : AppColors.chipBorder,
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            isDriver ? Icons.drive_eta_rounded : Icons.hail_rounded,
            size: 13,
            color: isDriver ? AppColors.dockItemActive : AppColors.textPrimary,
          ),
          const SizedBox(width: 4),
          Text(
            isDriver ? 'Motorista' : 'Passageiro',
            style: TextStyle(
              fontSize: 11.5,
              fontWeight: FontWeight.w700,
              color: isDriver
                  ? AppColors.dockItemActive
                  : AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
