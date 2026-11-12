// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import 'chat_panel_button.dart';

/// Botão "Copiar código PIX". Fica desabilitado e mostra progresso enquanto copia.
class CopyPixButton extends StatefulWidget {
  final String pixCode;

  const CopyPixButton({super.key, required this.pixCode});

  @override
  State<CopyPixButton> createState() => _CopyPixButtonState();
}

class _CopyPixButtonState extends State<CopyPixButton> {
  bool _copying = false;

  Future<void> _copy() async {
    if (_copying) return;
    HapticFeedback.mediumImpact();
    setState(() => _copying = true);

    var copied = false;
    try {
      await Clipboard.setData(ClipboardData(text: widget.pixCode));
      copied = true;
    } catch (_) {
      copied = false;
    } finally {
      if (mounted) setState(() => _copying = false);
    }
    if (!mounted) return;

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            copied
                ? 'Código PIX copiado. Cole no app do seu banco.'
                : 'Não foi possível copiar o código PIX. Tente novamente.',
            style: const TextStyle(
              fontWeight: FontWeight.w600,
              color: AppColors.textLight,
            ),
          ),
          backgroundColor: copied ? AppColors.primary : AppColors.error,
          behavior: SnackBarBehavior.floating,
        ),
      );
  }

  @override
  Widget build(BuildContext context) => ChatPanelButton(
    label: 'Copiar código PIX',
    icon: Icons.copy_rounded,
    busy: _copying,
    onPressed: _copy,
  );
}
