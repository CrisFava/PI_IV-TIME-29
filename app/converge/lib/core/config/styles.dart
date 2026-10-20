import 'package:flutter/material.dart';

abstract final class AppColors {
  // Primárias e Superfícies Escuras
  static const Color primary = Color(0xFF111111);
  static const Color dockBackground = Color(0xFF111111);
  static const Color carButtonResting = Color(0xFF14171A);
  static const Color carButtonDepressed = Color(0xFF1E232A);

  // Fundos e Superfícies Claras
  static const Color scaffoldBackground = Color(0xFFF8FAFC);
  static const Color cardBackground = Color(0xFFFFFFFF);
  static const Color cardSecondary = Color(0xFFF8FAFC);
  static const Color neutralLight = Color(0xFFF1F5F9);

  // Bordas e Linhas de Separação
  static const Color borderSubtle = Color(0xFFE2E8F0);
  static const Color borderCardPressed = Color(0xFF111111);
  static const Color routeLine = Color(0xFFCBD5E1);

  // Tipografia e Ícones
  static const Color textPrimary = Color(0xFF111111);
  static const Color textSecondary = Color(0xFF64748B);
  static const Color textMuted = Color(0xFF94A3B8);
  static const Color textLight = Colors.white;

  // Itens de Navegação (Dock)
  static const Color dockItemActive = Colors.white;
  static const Color dockItemInactive = Color(0x70FFFFFF); // alpha: 0.44
  static const Color dockIndicatorDot = Colors.white;

  // Barreira Translúcida de Dismiss
  static final Color barrier = Colors.black.withValues(alpha: 0.28);
}
