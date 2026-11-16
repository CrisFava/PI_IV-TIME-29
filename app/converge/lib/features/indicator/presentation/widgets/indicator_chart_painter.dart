// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

/// Desenha a série: barras (o último período em destaque) ou linha com pontos.
/// Cada valor ocupa uma coluna de mesma largura, alinhada com os rótulos embaixo.
class IndicatorChartPainter extends CustomPainter {
  final List<double> values;
  final bool asLine;
  final Color color;
  final Color mutedColor;

  const IndicatorChartPainter({
    required this.values,
    required this.asLine,
    required this.color,
    required this.mutedColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (values.isEmpty) return;
    final highest = values.reduce((a, b) => a > b ? a : b);
    // Sem nenhum valor, tudo fica na base (e não divide por zero)
    final scale = highest <= 0 ? 0.0 : size.height / highest;
    final column = size.width / values.length;

    if (asLine) {
      _paintLine(canvas, size, column, scale);
    } else {
      _paintBars(canvas, size, column, scale);
    }
  }

  void _paintBars(Canvas canvas, Size size, double column, double scale) {
    final barWidth = column * 0.55;
    for (var i = 0; i < values.length; i++) {
      // Barra mínima de 4 px: o período zerado continua visível
      final height = (values[i] * scale).clamp(4.0, size.height);
      final left = i * column + (column - barWidth) / 2;
      final rect = RRect.fromRectAndRadius(
        Rect.fromLTWH(left, size.height - height, barWidth, height),
        const Radius.circular(6),
      );
      final isLast = i == values.length - 1;
      canvas.drawRRect(rect, Paint()..color = isLast ? color : mutedColor);
    }
  }

  void _paintLine(Canvas canvas, Size size, double column, double scale) {
    final points = [
      for (var i = 0; i < values.length; i++)
        Offset(i * column + column / 2, size.height - values[i] * scale),
    ];

    final path = Path()..moveTo(points.first.dx, points.first.dy);
    for (final point in points.skip(1)) {
      path.lineTo(point.dx, point.dy);
    }
    canvas.drawPath(
      path,
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.5
        ..strokeJoin = StrokeJoin.round,
    );
    for (final point in points) {
      canvas.drawCircle(point, 3.5, Paint()..color = color);
    }
  }

  @override
  bool shouldRepaint(IndicatorChartPainter oldDelegate) =>
      oldDelegate.values != values || oldDelegate.asLine != asLine;
}
