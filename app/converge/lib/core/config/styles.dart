import 'package:flutter/material.dart';

abstract final class AppColors {
  // Primárias e Superfícies Escuras
  static const Color primary = Color(0xFF111111);
  static const Color primaryDark = Color(0xFF14171A);
  static const Color dockBackground = Color(0xFF111111);
  static const Color carButtonResting = Color(0xFF14171A);
  static const Color carButtonDepressed = Color(0xFF1E232A);

  // Fundos e Superfícies Claras
  static const Color scaffoldBackground = Color(0xFFF8FAFC);
  static const Color cardBackground = Color(0xFFFFFFFF);
  static const Color cardSecondary = Color(0xFFF8FAFC);
  static const Color neutralLight = Color(0xFFF1F5F9);
  static const Color inputBackground = Color(0xFFF8FAFC);
  static const Color buttonSecondary = Color(0xFFF3F4F6);
  static const Color chipBackground = Color(0xFFF5F5F5);

  // Bordas e Divisores
  static const Color borderSubtle = Color(0xFFE2E8F0);
  static const Color borderCardPressed = Color(0xFF111111);
  static const Color borderMedium = Color(0xFFCCCCCC);
  static const Color borderLight = Color(0xFFE5E5E5);
  static const Color footerBorder = Color(0xFFF2F2F2);
  static const Color chipBorder = Color(0xFFE0E0E0);
  static const Color routeLine = Color(0xFFCBD5E1);
  static const Color progressTrack = Color(0xFFEEEEEE);

  // Tipografia e Ícones
  static const Color textPrimary = Color(0xFF111111);
  static const Color textDark = Color(0xFF444444);
  static const Color textMedium = Color(0xFF666666);
  static const Color textSecondary = Color(0xFF64748B);
  static const Color textBodySecondary = Color(0xFF6B7280);
  static const Color textMuted = Color(0xFF94A3B8);
  static const Color textPlaceholder = Color(0xFF999999);
  static const Color textLight = Colors.white;
  static const Color iconMuted = Color(0xFF9CA3AF);

  // Estados e Feedback (Erro & Sucesso)
  static const Color error = Color(0xFFDC2626);
  static const Color errorBackground = Color(0xFFFEF2F2);
  static const Color errorBorder = Color(0xFFFECACA);
  static const Color errorText = Color(0xFFB91C1C);

  static const Color success = Color(0xFF15803D);
  static const Color successBackground = Color(0xFFF0FDF4);
  static const Color successBorder = Color(0xFFBBF7D0);

  // Elementos do Mapa Estilo Uber (Cartografia Monocromática com Alto Contraste)
  static const Color mapBackground = Color(0xFFD6DADF); // Fundo cinza arquitetural contrastante
  static const Color mapRoadPrimary = Color(0xFFFFFFFF); // Ruas em branco puro
  static const Color mapRoadSecondary = Color(0xFFE5E8EC);
  static const Color mapRoute = Color(0xFF111111);
  static const Color mapBuilding = Color(0xFFBCC2C9); // Prédios e estruturas mais escuras
  static const Color mapPinMarker = Color(0xFF111111);
  static const Color mapPinOrigin = Color(0xFF111111); // Bolinha preta opaca de Origem
  static const Color mapPinDestination = Color(0xFF111111); // Chegada quadriculada monocromática
  static const Color mapWater = Color(0xFFB0B6BF); // Água em cinza grafite médio
  static const Color driverApproachRoute = Color(0xFF94A3B8); // Cinza suave para trajeto do motorista

  // Itens de Navegação (Dock)
  static const Color dockItemActive = Colors.white;
  static const Color dockItemInactive = Color(0xFFA1A1AA); // Mais claro e legível quando não selecionado
  static const Color dockIndicatorDot = Colors.white;

  // Barreira Translúcida de Dismiss
  static final Color barrier = Colors.black.withValues(alpha: 0.28);
}
