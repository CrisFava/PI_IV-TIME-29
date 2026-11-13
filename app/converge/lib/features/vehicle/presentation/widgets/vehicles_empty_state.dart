// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';

/// Sem carros: avisa do bloqueio e oferece o cadastro.
class VehiclesEmptyState extends StatelessWidget {
  final VoidCallback onAdd;

  const VehiclesEmptyState({super.key, required this.onAdd});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.errorBackground,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.errorBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Row(
            children: [
              Icon(Icons.lock_rounded, size: 18, color: AppColors.errorText),
              SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Acesso de motorista bloqueado',
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w800,
                    color: AppColors.errorText,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          const Text(
            'Adicione um carro para oferecer caronas e ver pedidos de vaga.',
            style: TextStyle(fontSize: 12.5, color: AppColors.errorText),
          ),
          const SizedBox(height: 10),
          FilledButton.icon(
            onPressed: () {
              HapticFeedback.lightImpact();
              onAdd();
            },
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.dockItemActive,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            icon: const Icon(Icons.add_rounded, size: 18),
            label: const Text(
              'Adicionar carro',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}
