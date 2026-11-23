// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../config/styles.dart';

/// Os ícones de percurso usados em todos os mapas do app: a bolinha de partida e a
/// bandeira de chegada (também a de cada parada). Desenhados uma vez e guardados.
abstract final class MapMarkerIcons {
  static Future<BitmapDescriptor>? _origin;
  static Future<BitmapDescriptor>? _flag;
  static Future<BitmapDescriptor>? _flagLater;
  static Future<BitmapDescriptor>? _pickup;
  static Future<BitmapDescriptor>? _pickupLater;

  /// Bolinha preta com borda branca: de onde sai.
  static Future<BitmapDescriptor> origin() => _origin ??= _draw((canvas, c) {
    canvas.drawCircle(c, 16, Paint()..color = AppColors.cardBackground);
    canvas.drawCircle(c, 12, Paint()..color = AppColors.primary);
    canvas.drawCircle(c, 3.5, Paint()..color = AppColors.cardBackground);
  });

  /// Bandeira de chegada: preta (o destino e a próxima parada) ou cinza (as
  /// paradas depois da próxima).
  static Future<BitmapDescriptor> flag({bool highlighted = true}) => highlighted
      ? _flag ??= _drawFlag(AppColors.primary)
      : _flagLater ??= _drawFlag(AppColors.stopMarkerLater);

  /// Embarque de um passageiro: uma pessoa, preta (o próximo) ou cinza (os
  /// seguintes). Diferencia das bolinhas de partida e das bandeiras de chegada.
  static Future<BitmapDescriptor> pickup({bool highlighted = true}) =>
      highlighted
      ? _pickup ??= _drawIcon(AppColors.primary, Icons.person_rounded)
      : _pickupLater ??= _drawIcon(
          AppColors.stopMarkerLater,
          Icons.person_rounded,
        );

  static Future<BitmapDescriptor> _drawFlag(Color fill) =>
      _drawIcon(fill, Icons.flag_rounded);

  static Future<BitmapDescriptor> _drawIcon(Color fill, IconData icon) => _draw(
    (canvas, c) {
      canvas.drawCircle(c, 18, Paint()..color = AppColors.cardBackground);
      canvas.drawCircle(c, 15, Paint()..color = fill);
      final painter = TextPainter(
        text: TextSpan(
          text: String.fromCharCode(icon.codePoint),
          style: TextStyle(
            fontSize: 18,
            fontFamily: icon.fontFamily,
            package: icon.fontPackage,
            color: AppColors.textLight,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      painter.paint(canvas, c - Offset(painter.width / 2, painter.height / 2));
    },
  );

  static Future<BitmapDescriptor> _draw(
    void Function(Canvas canvas, Offset center) paint,
  ) async {
    const size = 64.0;
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder, const Rect.fromLTWH(0, 0, size, size));
    const center = Offset(size / 2, size / 2);
    canvas.drawCircle(
      center + const Offset(0, 2),
      18,
      Paint()
        ..color = AppColors.mapMarkerShadow
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4),
    );
    paint(canvas, center);
    final image = await recorder.endRecording().toImage(
      size.toInt(),
      size.toInt(),
    );
    final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
    return BitmapDescriptor.bytes(bytes!.buffer.asUint8List());
  }
}
