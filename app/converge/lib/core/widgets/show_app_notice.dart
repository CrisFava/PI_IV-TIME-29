// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import 'app_notice_banner.dart';
import 'app_notice_type.dart';

export 'app_notice_type.dart';

// Aviso que está na tela: um novo substitui o anterior
OverlayEntry? _currentNotice;

/// Mostra um aviso no topo da tela, com botão de fechar. Use no lugar do
/// SnackBar, que aparece embaixo e cobre a barra de navegação.
void showAppNotice(
  BuildContext context,
  String message, {
  AppNoticeType type = AppNoticeType.info,
  Duration duration = const Duration(seconds: 4),
}) {
  final overlay = Overlay.maybeOf(context, rootOverlay: true);
  if (overlay == null) return;

  hideAppNotice();

  late final OverlayEntry entry;
  entry = OverlayEntry(
    builder: (context) => Positioned(
      top: MediaQuery.paddingOf(context).top + 8,
      left: 12,
      right: 12,
      child: SafeArea(
        top: false,
        bottom: false,
        child: AppNoticeBanner(
          key: UniqueKey(),
          message: message,
          type: type,
          duration: duration,
          onDismissed: () {
            if (_currentNotice == entry) _currentNotice = null;
            if (entry.mounted) entry.remove();
          },
        ),
      ),
    ),
  );
  _currentNotice = entry;
  overlay.insert(entry);
}

/// Tira da tela o aviso atual, se houver.
void hideAppNotice() {
  final current = _currentNotice;
  _currentNotice = null;
  if (current != null && current.mounted) current.remove();
}
