import 'package:flutter/material.dart';

import '../../../../core/utils/validators.dart';
import 'onboarding_design_tokens.dart';

/// STEP 1: Identificação (E-mail Inteligente).
class RegisterStepEmail extends StatelessWidget {
  static const List<String> suggestedDomains = [
    '@puccampinas.edu.br',
    '@unicamp.br'
  ];

  final TextEditingController emailController;
  final FocusNode emailFocusNode;
  final bool isLoading;
  final ValueChanged<String> onDomainChipSelected;
  final VoidCallback onSubmitStep;

  const RegisterStepEmail({
    super.key,
    required this.emailController,
    required this.emailFocusNode,
    required this.isLoading,
    required this.onDomainChipSelected,
    required this.onSubmitStep,
  });

  @override
  Widget build(BuildContext context) {
    final currentText = emailController.text;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Qual é o seu e-mail acadêmico?',
          style: OnboardingDesignTokens.stepTitleStyle,
        ),
        const SizedBox(height: 8),
        const Text(
          'Usaremos ele para identificar sua universidade automaticamente.',
          style: OnboardingDesignTokens.stepSubtitleStyle,
        ),
        const SizedBox(height: 28),
        const Text(
          'E-mail',
          style: OnboardingDesignTokens.fieldLabelStyle,
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: emailController,
          focusNode: emailFocusNode,
          enabled: !isLoading,
          autofocus: true,
          keyboardType: TextInputType.emailAddress,
          textInputAction: TextInputAction.done,
          autocorrect: false,
          style: OnboardingDesignTokens.inputTextStyle,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          validator: Validators.email,
          onFieldSubmitted: (_) => onSubmitStep(),
          decoration: OnboardingDesignTokens.inputDecoration(
            hintText: 'ana@universidade.edu',
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: suggestedDomains.map((domain) {
            final isSelected =
                currentText.toLowerCase().endsWith(domain.toLowerCase());
            return _DomainQuickChip(
              domain: domain,
              isSelected: isSelected,
              onTap: isLoading ? null : () => onDomainChipSelected(domain),
            );
          }).toList(),
        ),
      ],
    );
  }
}

class _DomainQuickChip extends StatelessWidget {
  final String domain;
  final bool isSelected;
  final VoidCallback? onTap;

  const _DomainQuickChip({
    required this.domain,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: isSelected
          ? OnboardingDesignTokens.primary
          : OnboardingDesignTokens.chipBackground,
      borderRadius: BorderRadius.circular(999),
      child: InkWell(
        borderRadius: BorderRadius.circular(999),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(999),
            border: Border.all(
              color: isSelected
                  ? OnboardingDesignTokens.primary
                  : OnboardingDesignTokens.chipBorder,
            ),
          ),
          child: Text(
            domain,
            style: TextStyle(
              fontFamily: OnboardingDesignTokens.fontFamily,
              fontFamilyFallback: OnboardingDesignTokens.fontFamilyFallback,
              fontSize: 13,
              fontWeight: FontWeight.w500,
              color: isSelected
                  ? OnboardingDesignTokens.background
                  : OnboardingDesignTokens.primary,
            ),
          ),
        ),
      ),
    );
  }
}
