// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Botão compacto das faixas da conversa. Fica desabilitado e mostra o
/// progresso enquanto [busy] for `true`.
class ChatPanelButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final VoidCallback? onPressed;
  final bool primary;
  final bool busy;

  const ChatPanelButton({
    super.key,
    required this.label,
    required this.icon,
    required this.onPressed,
    this.primary = true,
    this.busy = false,
  });

  @override
  Widget build(BuildContext context) {
    final foreground = primary ? AppColors.textLight : AppColors.textPrimary;
    final leading = busy
        ? SizedBox(
            width: 14,
            height: 14,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: primary ? AppColors.iconMuted : AppColors.textPrimary,
            ),
          )
        : Icon(icon, size: 16);
    final text = Text(
      label,
      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700),
    );
    const padding = EdgeInsets.symmetric(horizontal: 14, vertical: 10);
    final shape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(12),
    );

    return primary
        ? FilledButton.icon(
            onPressed: busy ? null : onPressed,
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: foreground,
              disabledBackgroundColor: AppColors.buttonSecondary,
              disabledForegroundColor: AppColors.iconMuted,
              padding: padding,
              shape: shape,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            icon: leading,
            label: text,
          )
        : OutlinedButton.icon(
            onPressed: busy ? null : onPressed,
            style: OutlinedButton.styleFrom(
              foregroundColor: foreground,
              side: const BorderSide(color: AppColors.borderMedium),
              padding: padding,
              shape: shape,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            icon: leading,
            label: text,
          );
  }
}
