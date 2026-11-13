// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/widgets/show_app_notice.dart';
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

    // Só a falha avisa; a cópia bem-sucedida não precisa de aviso
    if (!copied) {
      showAppNotice(
        context,
        'Não foi possível copiar o código PIX. Tente novamente.',
        type: AppNoticeType.error,
      );
    }
  }

  @override
  Widget build(BuildContext context) => ChatPanelButton(
    label: 'Copiar código PIX',
    icon: Icons.copy_rounded,
    busy: _copying,
    onPressed: _copy,
  );
}
