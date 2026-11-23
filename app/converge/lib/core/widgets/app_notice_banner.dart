// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../config/styles.dart';
import 'app_notice_type.dart';

/// Aviso que desce do topo da tela, com botão de fechar, e some sozinho
/// depois de [duration]. Não cobre a barra de navegação de baixo.
class AppNoticeBanner extends StatefulWidget {
  final String message;
  final AppNoticeType type;
  final Duration duration;
  final VoidCallback onDismissed;

  const AppNoticeBanner({
    super.key,
    required this.message,
    required this.onDismissed,
    this.type = AppNoticeType.info,
    this.duration = const Duration(seconds: 4),
  });

  @override
  State<AppNoticeBanner> createState() => _AppNoticeBannerState();
}

class _AppNoticeBannerState extends State<AppNoticeBanner>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 260),
    reverseDuration: const Duration(milliseconds: 200),
  );
  late final Animation<Offset> _slide = Tween(
    begin: const Offset(0, -1.4),
    end: Offset.zero,
  ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic));

  Timer? _autoClose;
  bool _closing = false;

  @override
  void initState() {
    super.initState();
    _controller.forward();
    _autoClose = Timer(widget.duration, _close);
  }

  @override
  void dispose() {
    _autoClose?.cancel();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _close() async {
    if (_closing || !mounted) return;
    _closing = true;
    _autoClose?.cancel();
    await _controller.reverse();
    widget.onDismissed();
  }

  @override
  Widget build(BuildContext context) {
    final (background, icon) = switch (widget.type) {
      AppNoticeType.success => (AppColors.primary, Icons.check_circle_rounded),
      AppNoticeType.error => (AppColors.error, Icons.error_rounded),
      AppNoticeType.info => (AppColors.primary, Icons.info_rounded),
    };

    return SlideTransition(
      position: _slide,
      child: FadeTransition(
        opacity: _controller,
        child: Material(
          color: background,
          elevation: 6,
          shadowColor: AppColors.barrier,
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(14, 6, 4, 6),
            child: Row(
              children: [
                Icon(
                  icon,
                  size: 20,
                  color: widget.type == AppNoticeType.success
                      ? AppColors.successBorder
                      : AppColors.textLight,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Text(
                      widget.message,
                      style: const TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w600,
                        color: AppColors.textLight,
                        height: 1.3,
                      ),
                    ),
                  ),
                ),
                IconButton(
                  key: const ValueKey('close_app_notice'),
                  tooltip: 'Fechar aviso',
                  visualDensity: VisualDensity.compact,
                  onPressed: () {
                    HapticFeedback.lightImpact();
                    _close();
                  },
                  icon: const Icon(
                    Icons.close_rounded,
                    size: 20,
                    color: AppColors.textLight,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
