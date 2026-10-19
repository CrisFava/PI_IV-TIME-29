import 'package:flutter/material.dart';

import 'onboarding_design_tokens.dart';

/// Header fixo do fluxo de criação de conta (Shell):
/// - Botão de voltar (`←`) à esquerda
/// - Indicador discreto "Passo X de 3" à direita
/// - Barra de progresso horizontal contínua e fina (4px) com transição animada
class RegisterShellHeader extends StatelessWidget {
  final int currentStep;
  final int totalSteps;
  final VoidCallback? onBack;

  const RegisterShellHeader({
    super.key,
    required this.currentStep,
    this.totalSteps = 3,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    final targetProgress = (currentStep / totalSteps).clamp(0.0, 1.0);

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                onPressed: onBack,
                tooltip: 'Voltar',
                style: IconButton.styleFrom(
                  foregroundColor: OnboardingDesignTokens.primary,
                  padding: const EdgeInsets.all(8),
                ),
                icon: const Icon(
                  Icons.arrow_back_rounded,
                  size: 22,
                  color: OnboardingDesignTokens.primary,
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(right: 12),
                child: Text(
                  'Passo $currentStep de $totalSteps',
                  style: const TextStyle(
                    fontFamily: OnboardingDesignTokens.fontFamily,
                    fontFamilyFallback:
                        OnboardingDesignTokens.fontFamilyFallback,
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: OnboardingDesignTokens.subtitle,
                  ),
                ),
              ),
            ],
          ),
        ),
        SizedBox(
          height: 4,
          child: ColoredBox(
            color: OnboardingDesignTokens.progressTrack,
            child: TweenAnimationBuilder<double>(
              tween: Tween<double>(begin: 1 / totalSteps, end: targetProgress),
              duration: const Duration(milliseconds: 250),
              curve: Curves.easeOut,
              builder: (context, value, _) {
                return Align(
                  alignment: Alignment.centerLeft,
                  child: FractionallySizedBox(
                    widthFactor: value,
                    heightFactor: 1.0,
                    child: const ColoredBox(
                      color: OnboardingDesignTokens.primary,
                    ),
                  ),
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}
