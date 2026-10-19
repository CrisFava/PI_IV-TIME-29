import 'package:flutter/material.dart';

import '../../../../core/utils/validators.dart';
import 'onboarding_design_tokens.dart';

/// STEP 3: Vínculo Acadêmico e Segurança.
class RegisterStepAcademicSecurity extends StatelessWidget {
  static const List<String> suggestedCourses = [
    'Eng. Software',
    'Ciência da Computação',
    'Sistemas de Informação',
    'Eng. de Computação',
    'Eng. Civil',
    'Eng. Elétrica',
    'Eng. Mecânica',
    'Eng. de Produção',
    'Administração',
    'Arquitetura e Urbanismo',
    'Direito',
    'Medicina',
    'Psicologia',
  ];

  final TextEditingController universityController;
  final TextEditingController courseController;
  final TextEditingController enrollmentController;
  final TextEditingController passwordController;
  final FocusNode universityFocusNode;
  final FocusNode courseFocusNode;
  final FocusNode enrollmentFocusNode;
  final FocusNode passwordFocusNode;
  final bool isUniversityIdentifiedByEmail;
  final bool obscurePassword;
  final bool isLoading;
  final VoidCallback onTogglePasswordVisibility;
  final VoidCallback onSubmitStep;

  const RegisterStepAcademicSecurity({
    super.key,
    required this.universityController,
    required this.courseController,
    required this.enrollmentController,
    required this.passwordController,
    required this.universityFocusNode,
    required this.courseFocusNode,
    required this.enrollmentFocusNode,
    required this.passwordFocusNode,
    required this.isUniversityIdentifiedByEmail,
    required this.obscurePassword,
    required this.isLoading,
    required this.onTogglePasswordVisibility,
    required this.onSubmitStep,
  });

  static bool hasMinLength(String password) => password.length >= 8;
  static bool hasNumber(String password) => RegExp(r'\d').hasMatch(password);
  static bool hasSpecialChar(String password) =>
      RegExp(r'[^A-Za-z0-9]').hasMatch(password);
  static bool isPasswordStrong(String password) =>
      hasMinLength(password) &&
      hasNumber(password) &&
      hasSpecialChar(password);

  @override
  Widget build(BuildContext context) {
    final password = passwordController.text;
    final minLenOk = hasMinLength(password);
    final numberOk = hasNumber(password);
    final specialOk = hasSpecialChar(password);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Seu vínculo e senha de acesso',
          style: OnboardingDesignTokens.stepTitleStyle,
        ),
        const SizedBox(height: 8),
        const Text(
          'Confirme os dados da sua instituição e defina uma senha segura.',
          style: OnboardingDesignTokens.stepSubtitleStyle,
        ),
        const SizedBox(height: 28),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const Text(
              'Universidade',
              style: OnboardingDesignTokens.fieldLabelStyle,
            ),
            if (isUniversityIdentifiedByEmail &&
                universityController.text.trim().isNotEmpty)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: OnboardingDesignTokens.successBackground,
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(
                    color: OnboardingDesignTokens.successBorder,
                  ),
                ),
                child: const Text(
                  '✓ Identificada pelo e-mail',
                  style: TextStyle(
                    fontFamily: OnboardingDesignTokens.fontFamily,
                    fontFamilyFallback:
                        OnboardingDesignTokens.fontFamilyFallback,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: OnboardingDesignTokens.successGreen,
                  ),
                ),
              ),
          ],
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: universityController,
          focusNode: universityFocusNode,
          enabled: !isLoading,
          keyboardType: TextInputType.text,
          textInputAction: TextInputAction.next,
          style: OnboardingDesignTokens.inputTextStyle,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          validator: (v) => Validators.required(v, field: 'Universidade'),
          onFieldSubmitted: (_) => courseFocusNode.requestFocus(),
          decoration: OnboardingDesignTokens.inputDecoration(
            hintText: 'PUC-Campinas',
          ),
        ),
        const SizedBox(height: 18),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Curso',
                    style: OnboardingDesignTokens.fieldLabelStyle,
                  ),
                  const SizedBox(height: 8),
                  _CourseAutocompleteField(
                    controller: courseController,
                    focusNode: courseFocusNode,
                    nextFocusNode: enrollmentFocusNode,
                    enabled: !isLoading,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Matrícula',
                    style: OnboardingDesignTokens.fieldLabelStyle,
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: enrollmentController,
                    focusNode: enrollmentFocusNode,
                    enabled: !isLoading,
                    keyboardType: TextInputType.text,
                    textInputAction: TextInputAction.next,
                    style: OnboardingDesignTokens.inputTextStyle,
                    autovalidateMode: AutovalidateMode.onUserInteraction,
                    validator: (v) =>
                        Validators.required(v, field: 'Matrícula'),
                    onFieldSubmitted: (_) => passwordFocusNode.requestFocus(),
                    decoration: OnboardingDesignTokens.inputDecoration(
                      hintText: '2024-00123',
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 18),
        const Text(
          'Senha',
          style: OnboardingDesignTokens.fieldLabelStyle,
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: passwordController,
          focusNode: passwordFocusNode,
          enabled: !isLoading,
          obscureText: obscurePassword,
          textInputAction: TextInputAction.done,
          style: OnboardingDesignTokens.inputTextStyle,
          onFieldSubmitted: (_) => onSubmitStep(),
          validator: (value) {
            if (value == null || value.isEmpty) return 'Senha é obrigatória';
            if (!isPasswordStrong(value)) {
              return 'Atenda aos requisitos de segurança abaixo';
            }
            return null;
          },
          decoration: OnboardingDesignTokens.inputDecoration(
            hintText: '••••••••',
            suffixIcon: IconButton(
              onPressed: isLoading ? null : onTogglePasswordVisibility,
              tooltip: obscurePassword ? 'Mostrar senha' : 'Ocultar senha',
              icon: Icon(
                obscurePassword
                    ? Icons.visibility_outlined
                    : Icons.visibility_off_outlined,
                color: OnboardingDesignTokens.subtitle,
                size: 20,
              ),
            ),
          ),
        ),
        const SizedBox(height: 14),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _PasswordRequirementItem(
              label: 'Mínimo de 8 caracteres',
              isMet: minLenOk,
            ),
            const SizedBox(height: 8),
            _PasswordRequirementItem(
              label: 'Pelo menos 1 número',
              isMet: numberOk,
            ),
            const SizedBox(height: 8),
            _PasswordRequirementItem(
              label: 'Pelo menos 1 Caractere especial',
              isMet: specialOk,
            ),
          ],
        ),
      ],
    );
  }
}

class _CourseAutocompleteField extends StatelessWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final FocusNode nextFocusNode;
  final bool enabled;

  const _CourseAutocompleteField({
    required this.controller,
    required this.focusNode,
    required this.nextFocusNode,
    required this.enabled,
  });

  @override
  Widget build(BuildContext context) {
    return RawAutocomplete<String>(
      textEditingController: controller,
      focusNode: focusNode,
      optionsBuilder: (TextEditingValue textEditingValue) {
        final query = textEditingValue.text.trim().toLowerCase();
        if (query.isEmpty) {
          return RegisterStepAcademicSecurity.suggestedCourses;
        }
        return RegisterStepAcademicSecurity.suggestedCourses.where(
          (course) => course.toLowerCase().contains(query),
        );
      },
      onSelected: (_) => nextFocusNode.requestFocus(),
      fieldViewBuilder: (
        context,
        fieldController,
        fieldFocusNode,
        onFieldSubmitted,
      ) {
        return TextFormField(
          controller: fieldController,
          focusNode: fieldFocusNode,
          enabled: enabled,
          keyboardType: TextInputType.text,
          textInputAction: TextInputAction.next,
          style: OnboardingDesignTokens.inputTextStyle,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          validator: (v) => Validators.required(v, field: 'Curso'),
          onFieldSubmitted: (_) {
            onFieldSubmitted();
            nextFocusNode.requestFocus();
          },
          decoration: OnboardingDesignTokens.inputDecoration(
            hintText: 'Eng. Software',
            suffixIcon: const Icon(
              Icons.keyboard_arrow_down_rounded,
              color: OnboardingDesignTokens.subtitle,
              size: 20,
            ),
          ),
        );
      },
      optionsViewBuilder: (context, onSelected, options) {
        return Align(
          alignment: Alignment.topLeft,
          child: Material(
            elevation: 4,
            color: OnboardingDesignTokens.background,
            borderRadius: BorderRadius.circular(12),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 220, maxHeight: 200),
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(vertical: 4),
                shrinkWrap: true,
                itemCount: options.length,
                itemBuilder: (context, index) {
                  final option = options.elementAt(index);
                  return InkWell(
                    onTap: () => onSelected(option),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 10,
                      ),
                      child: Text(
                        option,
                        style: const TextStyle(
                          fontFamily: OnboardingDesignTokens.fontFamily,
                          fontFamilyFallback:
                              OnboardingDesignTokens.fontFamilyFallback,
                          fontSize: 13,
                          color: OnboardingDesignTokens.primary,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        );
      },
    );
  }
}

class _PasswordRequirementItem extends StatelessWidget {
  final String label;
  final bool isMet;

  const _PasswordRequirementItem({
    required this.label,
    required this.isMet,
  });

  @override
  Widget build(BuildContext context) {
    final iconColor = isMet
        ? OnboardingDesignTokens.successGreen
        : const Color(0xFF9CA3AF);
    final textColor = isMet
        ? OnboardingDesignTokens.successGreen
        : OnboardingDesignTokens.subtitle;

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 150),
          child: Icon(
            isMet ? Icons.check_circle_rounded : Icons.radio_button_unchecked,
            key: ValueKey<bool>(isMet),
            size: 16,
            color: iconColor,
          ),
        ),
        const SizedBox(width: 8),
        Text(
          label,
          style: TextStyle(
            fontFamily: OnboardingDesignTokens.fontFamily,
            fontFamilyFallback: OnboardingDesignTokens.fontFamilyFallback,
            fontSize: 13,
            fontWeight: isMet ? FontWeight.w600 : FontWeight.w400,
            color: textColor,
          ),
        ),
      ],
    );
  }
}
