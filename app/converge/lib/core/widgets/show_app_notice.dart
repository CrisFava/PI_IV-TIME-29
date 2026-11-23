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

  // Chave criada uma vez: o banner mantém a animação e o timer quando o
  // overlay é reconstruído (ex.: mudança na área segura)
  final bannerKey = UniqueKey();
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
          key: bannerKey,
          message: message,
          type: type,
          duration: duration,
          onDismissed: () {
            if (_currentNotice == entry) _currentNotice = null;
            _removeNotice(entry);
          },
        ),
      ),
    ),
  );
  _currentNotice = entry;
  _insertedNotices.add(entry);
  overlay.insert(entry);
}

/// Tira da tela o aviso atual, se houver.
void hideAppNotice() {
  final current = _currentNotice;
  _currentNotice = null;
  if (current != null) _removeNotice(current);
}

// Avisos inseridos e ainda não removidos. Não dá para confiar em `mounted`,
// que só fica true depois do primeiro frame do aviso.
final Set<OverlayEntry> _insertedNotices = {};

/// Remove e descarta o aviso uma única vez.
void _removeNotice(OverlayEntry entry) {
  if (!_insertedNotices.remove(entry)) return;
  entry
    ..remove()
    ..dispose();
}
