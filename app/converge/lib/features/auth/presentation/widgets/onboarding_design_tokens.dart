import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Tokens visuais do Design System para o fluxo de Onboarding / Sign Up.
abstract final class OnboardingDesignTokens {
  static const Color background = AppColors.cardBackground;
  static const Color primary = AppColors.primary;
  static const Color subtitle = AppColors.textMedium;
  static const Color microCopy = AppColors.textBodySecondary;
  static const Color inputBorder = AppColors.borderMedium;
  static const Color inputFocusBorder = AppColors.primary;
  static const Color progressTrack = AppColors.progressTrack;
  static const Color chipBackground = AppColors.chipBackground;
  static const Color chipBorder = AppColors.chipBorder;
  static const Color successGreen = AppColors.success;
  static const Color successBackground = AppColors.successBackground;
  static const Color successBorder = AppColors.successBorder;

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
    color: AppColors.textPlaceholder,
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
        borderSide: const BorderSide(color: AppColors.borderLight, width: 1),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: inputFocusBorder, width: 1.5),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: AppColors.error, width: 1),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: AppColors.error, width: 1.5),
      ),
    );
  }
}
