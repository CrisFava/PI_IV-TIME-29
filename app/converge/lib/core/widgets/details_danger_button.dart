// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../config/styles.dart';

/// Ação destrutiva no fim da folha de detalhes (cancelar carona, excluir veículo).
/// Fica com progresso e desabilitada enquanto a ação roda.
class DetailsDangerButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool isBusy;
  final VoidCallback onPressed;

  const DetailsDangerButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon = Icons.delete_outline_rounded,
    this.isBusy = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 24),
      child: OutlinedButton.icon(
        onPressed: isBusy
            ? null
            : () {
                HapticFeedback.mediumImpact();
                onPressed();
              },
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.error,
          disabledForegroundColor: AppColors.error,
          side: const BorderSide(color: AppColors.errorBorder),
          padding: const EdgeInsets.symmetric(vertical: 13),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
        icon: isBusy
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppColors.error,
                ),
              )
            : Icon(icon, size: 20),
        label: Text(label, style: const TextStyle(fontWeight: FontWeight.w700)),
      ),
    );
  }
}
