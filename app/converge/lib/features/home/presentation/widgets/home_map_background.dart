import 'dart:math' as math;
import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// ============================================================================
/// COMPONENTE: HomeMapBackground (Camada Base - Mapa em Tela Cheia)
/// ============================================================================
/// Mapa interativo vetorial estilo Uber, renderizado via Canvas de alto desempenho
/// (60/120 FPS), com suporte a gestos de arrasto e zoom (InteractiveViewer),
/// marcadores de universidade, carros em movimento e pulso de GPS.
/// ============================================================================
class HomeMapBackground extends StatefulWidget {
  final VoidCallback? onMapTap;
  final String universityName;

  const HomeMapBackground({
    super.key,
    this.onMapTap,
    this.universityName = 'PUC-Campinas',
  });

  @override
  State<HomeMapBackground> createState() => _HomeMapBackgroundState();
}

class _HomeMapBackgroundState extends State<HomeMapBackground>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulseController;
  late final TransformationController _transController;

  @override
  void initState() {
    super.initState();
    _transController = TransformationController();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2200),
    )..repeat();
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _transController.dispose();
    super.dispose();
  }

  void _recenterMap() {
    _transController.value = Matrix4.identity();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        // 1. Área Interativa do Mapa
        Positioned.fill(
          child: GestureDetector(
            onTap: widget.onMapTap,
            child: InteractiveViewer(
              transformationController: _transController,
              minScale: 0.8,
              maxScale: 2.8,
              boundaryMargin: const EdgeInsets.all(400),
              clipBehavior: Clip.none,
              child: AnimatedBuilder(
                animation: _pulseController,
                builder: (context, _) {
                  return CustomPaint(
                    size: const Size(1200, 1600),
                    painter: _UberStyleMapPainter(
                      pulseProgress: _pulseController.value,
                      universityName: widget.universityName,
                    ),
                  );
                },
              ),
            ),
          ),
        ),

        // 2. Botão Flutuante de Recentralização (Substituto minimalista de zoom)
        Positioned(
          right: 16,
          bottom: 270,
          child: Material(
            color: AppColors.cardBackground,
            elevation: 4,
            shadowColor: Colors.black.withValues(alpha: 0.15),
            shape: const CircleBorder(),
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: _recenterMap,
              child: Container(
                width: 44,
                height: 44,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: AppColors.borderSubtle, width: 1),
                ),
                child: const Icon(
                  Icons.my_location_rounded,
                  size: 20,
                  color: AppColors.textPrimary,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _UberStyleMapPainter extends CustomPainter {
  final double pulseProgress;
  final String universityName;

  _UberStyleMapPainter({
    required this.pulseProgress,
    required this.universityName,
  });

  @override
  void paint(Canvas canvas, Size size) {
    // 1. Fundo do Mapa (Base cartográfica neutra estilo Uber)
    final bgPaint = Paint()..color = AppColors.mapBackground;
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height), bgPaint);

    // 2. Lago / Área Verde Universitária
    _drawWaterBody(canvas, size);

    // 3. Quarteirões Urbanos (Quadras e Prédios)
    _drawCityBlocks(canvas, size);

    // 4. Rede de Vias Secundárias (Linhas finas)
    _drawSecondaryRoads(canvas, size);

    // 5. Avenidas Principais (Vias largas brancas)
    _drawArterialAvenues(canvas, size);

    // 6. Rota Ativa de Carona (Traçado com destaque)
    _drawActiveRoute(canvas);

    // 7. Veículos Próximos (Carros em deslocamento)
    _drawMovingCars(canvas);

    // 8. Marcadores de Campus (Destinos acadêmicos)
    _drawCampusPins(canvas);

    // 9. Posição Atual do Usuário (Ponto GPS pulsante)
    _drawUserLocation(canvas);
  }

  void _drawWaterBody(Canvas canvas, Size size) {
    final waterPaint = Paint()
      ..color = AppColors.mapWater
      ..style = PaintingStyle.fill;

    final path = Path()
      ..moveTo(size.width * 0.72, size.height * 0.18)
      ..cubicTo(
        size.width * 0.85,
        size.height * 0.20,
        size.width * 0.95,
        size.height * 0.35,
        size.width * 0.82,
        size.height * 0.45,
      )
      ..cubicTo(
        size.width * 0.70,
        size.height * 0.52,
        size.width * 0.65,
        size.height * 0.38,
        size.width * 0.68,
        size.height * 0.25,
      )
      ..close();

    canvas.drawPath(path, waterPaint);
  }

  void _drawCityBlocks(Canvas canvas, Size size) {
    final blockPaint = Paint()
      ..color = AppColors.mapBuilding
      ..style = PaintingStyle.fill;

    const blockSize = 64.0;
    const spacing = 22.0;

    for (double x = 40; x < size.width; x += blockSize + spacing) {
      for (double y = 60; y < size.height; y += blockSize + spacing) {
        // Evita desenhar blocos sobre áreas de vias principais ou água
        if (x > size.width * 0.65 && y < size.height * 0.50) continue;

        final rect = RRect.fromRectAndRadius(
          Rect.fromLTWH(x, y, blockSize, blockSize),
          const Radius.circular(8),
        );
        canvas.drawRRect(rect, blockPaint);
      }
    }
  }

  void _drawSecondaryRoads(Canvas canvas, Size size) {
    final roadPaint = Paint()
      ..color = AppColors.mapRoadSecondary
      ..strokeWidth = 6.0
      ..style = PaintingStyle.stroke;

    // Vias verticais
    for (double x = 70; x < size.width; x += 86) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), roadPaint);
    }

    // Vias horizontais
    for (double y = 90; y < size.height; y += 86) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), roadPaint);
    }
  }

  void _drawArterialAvenues(Canvas canvas, Size size) {
    final shadowPaint = Paint()
      ..color = Colors.black.withValues(alpha: 0.05)
      ..strokeWidth = 18.0
      ..style = PaintingStyle.stroke;

    final avenuePaint = Paint()
      ..color = AppColors.mapRoadPrimary
      ..strokeWidth = 14.0
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    // Grande avenida diagonal
    final mainAve = Path()
      ..moveTo(80, size.height * 0.85)
      ..cubicTo(
        size.width * 0.35,
        size.height * 0.75,
        size.width * 0.55,
        size.height * 0.45,
        size.width * 0.88,
        size.height * 0.15,
      );

    // Eixo rodoviário transversal
    final transAve = Path()
      ..moveTo(0, size.height * 0.52)
      ..lineTo(size.width, size.height * 0.48);

    // Eixo norte-sul universitário
    final uniAve = Path()
      ..moveTo(size.width * 0.45, 0)
      ..lineTo(size.width * 0.45, size.height);

    canvas.drawPath(mainAve, shadowPaint);
    canvas.drawPath(transAve, shadowPaint);
    canvas.drawPath(uniAve, shadowPaint);

    canvas.drawPath(mainAve, avenuePaint);
    canvas.drawPath(transAve, avenuePaint);
    canvas.drawPath(uniAve, avenuePaint);
  }

  void _drawActiveRoute(Canvas canvas) {
    final routeGlow = Paint()
      ..color = AppColors.mapRoute.withValues(alpha: 0.15)
      ..strokeWidth = 12.0
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    final routePaint = Paint()
      ..color = AppColors.mapRoute
      ..strokeWidth = 5.0
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    final routePath = Path()
      ..moveTo(380, 560)
      ..lineTo(450, 560)
      ..lineTo(450, 410)
      ..cubicTo(500, 370, 580, 310, 680, 240);

    canvas.drawPath(routePath, routeGlow);
    canvas.drawPath(routePath, routePaint);
  }

  void _drawMovingCars(Canvas canvas) {
    _drawCarGlyph(canvas, const Offset(450, 480), headingAngle: -math.pi / 2);
    _drawCarGlyph(canvas, const Offset(590, 305), headingAngle: -0.6);
    _drawCarGlyph(canvas, const Offset(320, 560), headingAngle: 0.0);
  }

  void _drawCarGlyph(Canvas canvas, Offset pos, {required double headingAngle}) {
    canvas.save();
    canvas.translate(pos.dx, pos.dy);
    canvas.rotate(headingAngle);

    // Sombra do veículo
    final shadowPaint = Paint()
      ..color = Colors.black.withValues(alpha: 0.20)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 3);
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        const Rect.fromLTWH(-7, -13, 14, 26),
        const Radius.circular(5),
      ),
      shadowPaint,
    );

    // Corpo do carro (Monocromático no estilo Uber)
    final carBodyPaint = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.fill;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        const Rect.fromLTWH(-6, -12, 12, 24),
        const Radius.circular(4),
      ),
      carBodyPaint,
    );

    // Parabrisa dianteiro
    final glassPaint = Paint()
      ..color = Colors.white.withValues(alpha: 0.8)
      ..style = PaintingStyle.fill;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        const Rect.fromLTWH(-4, -8, 8, 4),
        const Radius.circular(1.5),
      ),
      glassPaint,
    );

    canvas.restore();
  }

  void _drawCampusPins(Canvas canvas) {
    // Pin Destino 1: Campus Principal
    _drawPinMarker(
      canvas,
      const Offset(680, 240),
      label: universityName.isNotEmpty ? universityName : 'Campus I',
    );

    // Pin Destino 2: Campus II
    _drawPinMarker(
      canvas,
      const Offset(220, 720),
      label: 'Campus II',
    );
  }

  void _drawPinMarker(Canvas canvas, Offset pos, {required String label}) {
    final circlePaint = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.fill;

    // Círculo base do pino
    canvas.drawCircle(pos, 10, circlePaint);
    canvas.drawCircle(pos, 5, Paint()..color = Colors.white);

    // Caixa de texto indicativo
    final textSpan = TextSpan(
      text: label,
      style: const TextStyle(
        color: AppColors.textPrimary,
        fontSize: 11,
        fontWeight: FontWeight.w700,
      ),
    );
    final textPainter = TextPainter(
      text: textSpan,
      textDirection: TextDirection.ltr,
    )..layout();

    final bgRect = RRect.fromRectAndRadius(
      Rect.fromCenter(
        center: Offset(pos.dx, pos.dy - 22),
        width: textPainter.width + 16,
        height: textPainter.height + 8,
      ),
      const Radius.circular(8),
    );

    canvas.drawRRect(
      bgRect,
      Paint()..color = Colors.white,
    );
    canvas.drawRRect(
      bgRect,
      Paint()
        ..color = AppColors.borderSubtle
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1,
    );

    textPainter.paint(
      canvas,
      Offset(pos.dx - textPainter.width / 2, pos.dy - 22 - textPainter.height / 2),
    );
  }

  void _drawUserLocation(Canvas canvas) {
    const userPos = Offset(380, 560);

    // 1. Halo pulsante externo
    final pulseRadius = 14.0 + (pulseProgress * 28.0);
    final pulseAlpha = (1.0 - pulseProgress).clamp(0.0, 0.45);
    final pulsePaint = Paint()
      ..color = AppColors.primary.withValues(alpha: pulseAlpha)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(userPos, pulseRadius, pulsePaint);

    // 2. Halo interno estático
    final haloPaint = Paint()
      ..color = AppColors.primary.withValues(alpha: 0.18)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(userPos, 14, haloPaint);

    // 3. Anel branco de contraste
    final whiteRingPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(userPos, 8, whiteRingPaint);

    // 4. Ponto central preto
    final centerDotPaint = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.fill;
    canvas.drawCircle(userPos, 5, centerDotPaint);
  }

  @override
  bool shouldRepaint(covariant _UberStyleMapPainter oldDelegate) {
    return oldDelegate.pulseProgress != pulseProgress ||
        oldDelegate.universityName != universityName;
  }
}
