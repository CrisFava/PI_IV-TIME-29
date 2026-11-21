// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Etiqueta que separa as mensagens de um dia das do dia anterior: "Hoje", "Ontem"
/// ou a data.
class ChatDaySeparator extends StatelessWidget {
  final String label;

  const ChatDaySeparator({super.key, required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Center(
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          decoration: BoxDecoration(
            color: AppColors.neutralLight,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Semantics(
            header: true,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w700,
                color: AppColors.textSecondary,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
