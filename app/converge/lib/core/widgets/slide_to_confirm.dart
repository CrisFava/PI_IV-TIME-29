// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../config/styles.dart';

/// "Deslize para confirmar": o motorista arrasta o botão até o fim da faixa.
/// Evita confirmar sem querer com um toque enquanto dirige. Soltar antes do fim
/// volta ao começo. Com [isBusy], mostra o carregamento e não aceita arraste.
class SlideToConfirm extends StatefulWidget {
  final String label;
  final IconData icon;
  final Color color;
  final bool isBusy;
  final VoidCallback onConfirmed;

  const SlideToConfirm({
    super.key,
    required this.label,
    required this.onConfirmed,
    this.icon = Icons.keyboard_double_arrow_right_rounded,
    this.color = AppColors.primary,
    this.isBusy = false,
  });

  @override
  State<SlideToConfirm> createState() => _SlideToConfirmState();
}

class _SlideToConfirmState extends State<SlideToConfirm>
    with SingleTickerProviderStateMixin {
  static const double _height = 56;
  static const double _thumb = 48;
  static const double _padding = 4;
  // Arrastar até 85% da faixa já confirma
  static const double _threshold = 0.85;

  double _dx = 0;
  late final AnimationController _reset = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 220),
  )..addListener(() => setState(() => _dx = _resetFrom * (1 - _reset.value)));
  double _resetFrom = 0;

  @override
  void dispose() {
    _reset.dispose();
    super.dispose();
  }

  void _goBack() {
    _resetFrom = _dx;
    _reset.forward(from: 0);
  }

  @override
  void didUpdateWidget(covariant SlideToConfirm oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Terminou de enviar: volta ao começo para a próxima confirmação
    // A etapa mudou (outro passageiro, outra ação): começa do início
    if ((oldWidget.isBusy && !widget.isBusy) ||
        oldWidget.label != widget.label) {
      _goBack();
    }
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final maxDx = constraints.maxWidth - _thumb - _padding * 2;
        final progress = maxDx <= 0 ? 0.0 : (_dx / maxDx).clamp(0.0, 1.0);

        return Semantics(
          button: true,
          label: widget.label,
          onTap: widget.isBusy ? null : widget.onConfirmed,
          child: Container(
            height: _height,
            decoration: BoxDecoration(
              color: widget.color.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(_height / 2),
              border: Border.all(color: widget.color.withValues(alpha: 0.35)),
            ),
            child: Stack(
              alignment: Alignment.centerLeft,
              children: [
                // O texto some aos poucos enquanto o botão é arrastado
                Center(
                  child: Opacity(
                    opacity: (1 - progress * 1.4).clamp(0.0, 1.0),
                    child: Padding(
                      padding: const EdgeInsets.only(left: _thumb),
                      child: Text(
                        widget.label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 14.5,
                          fontWeight: FontWeight.w800,
                          color: widget.color,
                        ),
                      ),
                    ),
                  ),
                ),
                Positioned(
                  left: _padding + _dx,
                  child: GestureDetector(
                    onHorizontalDragUpdate: widget.isBusy
                        ? null
                        : (details) {
                            _reset.stop();
                            setState(
                              () => _dx = (_dx + details.delta.dx).clamp(
                                0.0,
                                maxDx,
                              ),
                            );
                          },
                    onHorizontalDragEnd: widget.isBusy
                        ? null
                        : (_) {
                            if (progress >= _threshold) {
                              HapticFeedback.mediumImpact();
                              setState(() => _dx = maxDx);
                              widget.onConfirmed();
                              // Se a ação não ficar "enviando", volta sozinho
                              Future.delayed(
                                const Duration(milliseconds: 700),
                                () {
                                  if (mounted && !widget.isBusy) _goBack();
                                },
                              );
                            } else {
                              _goBack();
                            }
                          },
                    child: Container(
                      width: _thumb,
                      height: _thumb,
                      decoration: BoxDecoration(
                        color: widget.color,
                        shape: BoxShape.circle,
                      ),
                      child: widget.isBusy
                          ? const Padding(
                              padding: EdgeInsets.all(14),
                              child: CircularProgressIndicator(
                                strokeWidth: 2.2,
                                color: AppColors.textLight,
                              ),
                            )
                          : Icon(
                              widget.icon,
                              color: AppColors.textLight,
                              size: 26,
                            ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
