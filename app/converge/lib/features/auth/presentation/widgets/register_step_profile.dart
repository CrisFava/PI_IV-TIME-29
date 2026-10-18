import 'package:flutter/material.dart';

import '../../../../core/utils/phone_input_formatter.dart';
import '../../../../core/utils/validators.dart';
import 'onboarding_design_tokens.dart';

/// STEP 2: Perfil e Contato.
class RegisterStepProfile extends StatelessWidget {
  final TextEditingController nameController;
  final TextEditingController phoneController;
  final FocusNode nameFocusNode;
  final FocusNode phoneFocusNode;
  final bool isLoading;
  final VoidCallback onSubmitStep;

  const RegisterStepProfile({
    super.key,
    required this.nameController,
    required this.phoneController,
    required this.nameFocusNode,
    required this.phoneFocusNode,
    required this.isLoading,
    required this.onSubmitStep,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Prazer! Como podemos te chamar?',
          style: OnboardingDesignTokens.stepTitleStyle,
        ),
        const SizedBox(height: 8),
        const Text(
          'Precisamos dos seus dados básicos para criar seu perfil.',
          style: OnboardingDesignTokens.stepSubtitleStyle,
        ),
        const SizedBox(height: 28),
        const Text(
          'Nome completo',
          style: OnboardingDesignTokens.fieldLabelStyle,
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: nameController,
          focusNode: nameFocusNode,
          enabled: !isLoading,
          autofocus: true,
          keyboardType: TextInputType.name,
          textCapitalization: TextCapitalization.words,
          textInputAction: TextInputAction.next,
          style: OnboardingDesignTokens.inputTextStyle,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          validator: Validators.name,
          onFieldSubmitted: (_) => phoneFocusNode.requestFocus(),
          decoration: OnboardingDesignTokens.inputDecoration(
            hintText: 'Ex.: Ana Souza',
          ),
        ),
        const SizedBox(height: 20),
        const Text(
          'Telefone',
          style: OnboardingDesignTokens.fieldLabelStyle,
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: phoneController,
          focusNode: phoneFocusNode,
          enabled: !isLoading,
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.done,
          style: OnboardingDesignTokens.inputTextStyle,
          inputFormatters: const [PhoneInputFormatter()],
          autovalidateMode: AutovalidateMode.onUserInteraction,
          validator: Validators.phone,
          onFieldSubmitted: (_) => onSubmitStep(),
          decoration: OnboardingDesignTokens.inputDecoration(
            hintText: '(00) 90000-0000',
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Fique tranquilo, não enviamos spam.',
          style: OnboardingDesignTokens.microCopyStyle,
        ),
      ],
    );
  }
}
