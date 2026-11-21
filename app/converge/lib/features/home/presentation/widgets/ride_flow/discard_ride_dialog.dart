// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';

/// Pergunta antes de descartar um pedido de carona (ou uma carona) já começado,
/// para um toque sem querer no X não apagar o que a pessoa escolheu.
///
/// Devolve `true` só se ela escolher descartar; fechar o aviso conta como continuar.
Future<bool> showDiscardRideDialog(
  BuildContext context, {
  required bool isDriver,
}) async {
  final discard = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      backgroundColor: AppColors.cardBackground,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: Text(
        isDriver ? 'Descartar a carona?' : 'Descartar o pedido?',
        style: const TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.w800,
          color: AppColors.textPrimary,
        ),
      ),
      content: const Text(
        'O trajeto e o horário que você escolheu serão perdidos.',
        style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
      ),
      actions: [
        TextButton(
          key: const ValueKey('discard_ride_keep'),
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text(
            'Continuar',
            style: TextStyle(
              fontWeight: FontWeight.w600,
              color: AppColors.textSecondary,
            ),
          ),
        ),
        ElevatedButton(
          key: const ValueKey('discard_ride_confirm'),
          onPressed: () {
            HapticFeedback.mediumImpact();
            Navigator.of(dialogContext).pop(true);
          },
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.error,
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          child: const Text(
            'Descartar',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: AppColors.textLight,
            ),
          ),
        ),
      ],
    ),
  );
  return discard ?? false;
}
