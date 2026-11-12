// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Aviso centralizado na conversa, para eventos de pagamento e da viagem.
class ChatEventNotice extends StatelessWidget {
  final IconData icon;
  final String text;
  final String time;
  final bool success;

  const ChatEventNotice({
    super.key,
    required this.icon,
    required this.text,
    required this.time,
    this.success = false,
  });

  @override
  Widget build(BuildContext context) {
    final foreground = success ? AppColors.success : AppColors.textSecondary;

    return Center(
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
        decoration: BoxDecoration(
          color: success
              ? AppColors.successBackground
              : AppColors.cardBackground,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: success ? AppColors.successBorder : AppColors.borderSubtle,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 15, color: foreground),
            const SizedBox(width: 6),
            Flexible(
              child: Text(
                time.isEmpty ? text : '$text • $time',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: foreground,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
