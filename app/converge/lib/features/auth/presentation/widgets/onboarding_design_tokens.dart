import 'package:flutter/material.dart';

/// Tokens visuais do Design System para o fluxo de Onboarding / Sign Up.
abstract final class OnboardingDesignTokens {
  static const Color background = Color(0xFFFFFFFF);
  static const Color primary = Color(0xFF111111);
  static const Color subtitle = Color(0xFF666666);
  static const Color microCopy = Color(0xFF777777);
  static const Color inputBorder = Color(0xFFCCCCCC);
  static const Color inputFocusBorder = Color(0xFF111111);
  static const Color progressTrack = Color(0xFFEEEEEE);
  static const Color chipBackground = Color(0xFFF5F5F5);
  static const Color chipBorder = Color(0xFFE0E0E0);
  static const Color successGreen = Color(0xFF15803D);
  static const Color successBackground = Color(0xFFECFDF5);
  static const Color successBorder = Color(0xFFA7F3D0);

  static const String fontFamily = 'Inter';
  static const List<String> fontFamilyFallback = [
    'Plus Jakarta Sans',
    'Roboto',
    'sans-serif',
  ];

  static const TextStyle stepTitleStyle = TextStyle(
    fontFamily: fontFamily,
    fontFamilyFallback: fontFamilyFallback,
    fontSize: 24,
    fontWeight: FontWeight.w700,
    color: primary,
    height: 1.25,
    letterSpacing: -0.4,
  );

  static const TextStyle stepSubtitleStyle = TextStyle(
    fontFamily: fontFamily,
    fontFamilyFallback: fontFamilyFallback,
    fontSize: 14,
    fontWeight: FontWeight.w400,
    color: subtitle,
    height: 1.45,
  );

  static const TextStyle fieldLabelStyle = TextStyle(
    fontFamily: fontFamily,
    fontFamilyFallback: fontFamilyFallback,
    fontSize: 14,
    fontWeight: FontWeight.w500,
    color: primary,
    height: 1.3,
  );

  static const TextStyle inputTextStyle = TextStyle(
    fontFamily: fontFamily,
    fontFamilyFallback: fontFamilyFallback,
    fontSize: 15,
    fontWeight: FontWeight.w400,
    color: primary,
  );

  static const TextStyle placeholderStyle = TextStyle(
    fontFamily: fontFamily,
    fontFamilyFallback: fontFamilyFallback,
    fontSize: 15,
    fontWeight: FontWeight.w400,
    color: Color(0xFF999999),
  );

  static const TextStyle microCopyStyle = TextStyle(
    fontFamily: fontFamily,
    fontFamilyFallback: fontFamilyFallback,
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: microCopy,
    height: 1.4,
  );

  static InputDecoration inputDecoration({
    required String hintText,
    Widget? suffixIcon,
  }) {
    final borderRadius = BorderRadius.circular(12);
    return InputDecoration(
      hintText: hintText,
      hintStyle: placeholderStyle,
      filled: true,
      fillColor: background,
      isDense: false,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
      suffixIcon: suffixIcon,
      enabledBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: inputBorder, width: 1),
      ),
      disabledBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: Color(0xFFE5E5E5), width: 1),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: inputFocusBorder, width: 1.5),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: Color(0xFFDC2626), width: 1),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: Color(0xFFDC2626), width: 1.5),
      ),
    );
  }
}
