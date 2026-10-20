import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import 'onboarding_design_tokens.dart';

/// Footer fixo (Sticky CTA) ancorado acima do teclado virtual:
/// - Botão principal 100% largura, 52px altura, raio 14px, cor #111111,
///   texto branco centralizado e opacidade 0.4 quando desabilitado.
/// - Rodapé secundário exclusivo no Step 1 ("Já tem conta? Fazer login").
class RegisterStickyFooter extends StatelessWidget {
  final dynamic label;
  final bool isEnabled;
  final bool isLoading;
  final bool showLoginPrompt;
  final VoidCallback onPrimaryPressed;
  final VoidCallback onLoginPressed;

  const RegisterStickyFooter({
    super.key,
    required this.label,
    required this.isEnabled,
    required this.isLoading,
    required this.showLoginPrompt,
    required this.onPrimaryPressed,
    required this.onLoginPressed,
  });

  Widget _buildChild() {
    if (label is Widget) {
      return label as Widget;
    }
    if (label is IconData) {
      return Icon(
        label as IconData,
        size: 24,
        color: OnboardingDesignTokens.background,
      );
    }
    return Text(
      label.toString(),
      textAlign: TextAlign.center,
      style: const TextStyle(
        fontFamily: OnboardingDesignTokens.fontFamily,
        fontFamilyFallback:
            OnboardingDesignTokens.fontFamilyFallback,
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: OnboardingDesignTokens.background,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final canActivate = isEnabled && !isLoading;

    return Container(
      decoration: const BoxDecoration(
        color: OnboardingDesignTokens.background,
        border: Border(
          top: BorderSide(color: AppColors.footerBorder, width: 1),
        ),
      ),
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 16),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 440),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              AnimatedOpacity(
                duration: const Duration(milliseconds: 180),
                curve: Curves.easeOut,
                opacity: canActivate ? 1.0 : 0.4,
                child: SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: FilledButton(
                    style: FilledButton.styleFrom(
                      backgroundColor: OnboardingDesignTokens.primary,
                      foregroundColor: OnboardingDesignTokens.background,
                      disabledBackgroundColor: OnboardingDesignTokens.primary,
                      disabledForegroundColor:
                          OnboardingDesignTokens.background,
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                    ),
                    onPressed: canActivate ? onPrimaryPressed : null,
                    child: isLoading
                        ? const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: OnboardingDesignTokens.background,
                            ),
                          )
                        : _buildChild(),
                  ),
                ),
              ),
              if (showLoginPrompt) ...[
                const SizedBox(height: 12),
                Center(
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: isLoading ? null : onLoginPressed,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        vertical: 4,
                        horizontal: 8,
                      ),
                      child: Text.rich(
                        const TextSpan(
                          text: 'Já tem conta? ',
                          style: TextStyle(
                            fontFamily: OnboardingDesignTokens.fontFamily,
                            fontFamilyFallback:
                                OnboardingDesignTokens.fontFamilyFallback,
                            fontSize: 14,
                            fontWeight: FontWeight.w400,
                            color: OnboardingDesignTokens.subtitle,
                          ),
                          children: [
                            TextSpan(
                              text: 'Fazer login',
                              style: TextStyle(
                                fontWeight: FontWeight.w700,
                                color: OnboardingDesignTokens.primary,
                              ),
                            ),
                          ],
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
