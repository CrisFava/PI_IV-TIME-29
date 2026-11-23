// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/vehicle_model.dart';

/// Pergunta se o usuário tem certeza de apagar o carro. Sendo o último carro,
/// avisa também que o acesso de motorista será bloqueado. Devolve true para apagar.
Future<bool> confirmDeleteVehicle(
  BuildContext context, {
  required VehicleModel vehicle,
  required bool isLastVehicle,
}) async {
  HapticFeedback.mediumImpact();
  final confirmed = await showDialog<bool>(
    context: context,
    barrierColor: AppColors.barrier,
    builder: (context) => AlertDialog(
      backgroundColor: AppColors.cardBackground,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      icon: const Icon(
        Icons.delete_forever_rounded,
        color: AppColors.error,
        size: 32,
      ),
      title: Text(
        'Apagar ${vehicle.displayName}?',
        textAlign: TextAlign.center,
        style: const TextStyle(
          fontSize: 17,
          fontWeight: FontWeight.w800,
          color: AppColors.textPrimary,
        ),
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Tem certeza que quer apagar o carro ${vehicle.model} '
            '(${vehicle.formattedPlate})?',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 13.5,
              color: AppColors.textSecondary,
              height: 1.35,
            ),
          ),
          if (isLastVehicle) ...[
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.errorBackground,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.errorBorder),
              ),
              child: const Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    Icons.lock_rounded,
                    size: 18,
                    color: AppColors.errorText,
                  ),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Este é o seu único carro. Sem ele, o seu acesso de '
                      'motorista será bloqueado até você adicionar outro.',
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        color: AppColors.errorText,
                        height: 1.3,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
      actionsAlignment: MainAxisAlignment.spaceBetween,
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
          child: const Text(
            'Cancelar',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
        FilledButton(
          key: const ValueKey('confirm_delete_vehicle'),
          onPressed: () {
            HapticFeedback.mediumImpact();
            Navigator.of(context).pop(true);
          },
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.error,
            foregroundColor: AppColors.textLight,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          child: const Text(
            'Apagar',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
      ],
    ),
  );
  return confirmed ?? false;
}
