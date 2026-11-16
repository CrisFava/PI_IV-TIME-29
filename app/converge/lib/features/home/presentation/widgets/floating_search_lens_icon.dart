// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Ícone de lupa com animação fluida (movimento lateral + órbita suave de loading)
/// para sinalizar de forma agradável que a pesquisa no Google Maps está em andamento.
class FloatingSearchLensIcon extends StatefulWidget {
  const FloatingSearchLensIcon({super.key});

  @override
  State<FloatingSearchLensIcon> createState() => _FloatingSearchLensIconState();
}

class _FloatingSearchLensIconState extends State<FloatingSearchLensIcon>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1150),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        final angle = _controller.value * 2 * math.pi;
        // Movimento lateral principal + leve flutuação vertical (órbita de "inspeção/loading")
        final dx = math.sin(angle) * 4.2;
        final dy = math.cos(angle * 2) * 1.6;
        // Leve inclinação acompanhando o deslocamento horizontal
        final tilt = math.sin(angle) * 0.14;
        final scale = 0.96 + (math.cos(angle) * 0.06);

        return Transform.translate(
          offset: Offset(dx, dy),
          child: Transform.rotate(
            angle: tilt,
            child: Transform.scale(scale: scale, child: child),
          ),
        );
      },
      child: const Icon(
        Icons.search_rounded,
        size: 20,
        color: AppColors.primary,
      ),
    );
  }
}
