// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Bolinha verde com um anel que cresce e some, igual ao do botão do carro durante
/// a viagem, para dizer que a viagem está acontecendo agora. Com "reduzir animações"
/// ligado no aparelho, fica só a bolinha, parada.
class LiveDot extends StatefulWidget {
  const LiveDot({super.key});

  @override
  State<LiveDot> createState() => _LiveDotState();
}

class _LiveDotState extends State<LiveDot> with SingleTickerProviderStateMixin {
  static const double _size = 8;

  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1600),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (MediaQuery.disableAnimationsOf(context)) {
      _controller
        ..stop()
        ..value = 0;
    } else if (!_controller.isAnimating) {
      _controller.repeat();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final animated = !MediaQuery.disableAnimationsOf(context);

    return ExcludeSemantics(
      child: SizedBox(
        width: _size,
        height: _size,
        child: Stack(
          // O anel passa do tamanho da bolinha, sem empurrar o que está ao lado
          clipBehavior: Clip.none,
          alignment: Alignment.center,
          children: [
            if (animated)
              AnimatedBuilder(
                animation: _controller,
                builder: (context, _) {
                  final t = _controller.value;
                  return Transform.scale(
                    scale: 1 + (1.2 * t),
                    child: Container(
                      width: _size,
                      height: _size,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: AppColors.success.withValues(
                            alpha: 0.7 * (1 - t),
                          ),
                          width: 1.5,
                        ),
                      ),
                    ),
                  );
                },
              ),
            Container(
              width: _size,
              height: _size,
              decoration: const BoxDecoration(
                color: AppColors.success,
                shape: BoxShape.circle,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
