// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Aviso centralizado na conversa, para eventos de pagamento e da viagem.
class ChatEventNotice extends StatelessWidget {
  /// Ícone à esquerda do texto.
  final IconData icon;

  /// Texto do aviso.
  final String text;

  /// Horário já formatado; vazio para não mostrar.
  final String time;

  /// Usa as cores de sucesso (verde), como no pagamento confirmado.
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
