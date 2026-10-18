import 'package:converge/features/auth/presentation/widgets/action_button.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app_routes.dart';
import '../../../../core/utils/validators.dart';
import '../../state/auth_action_controller.dart';
import '../../state/inputs/profile_input.dart';
import '../../state/inputs/register_input.dart';
import '../widgets/onboarding_design_tokens.dart';
import '../widgets/register_shell_header.dart';
import '../widgets/register_step_academic_security.dart';
import '../widgets/register_step_email.dart';
import '../widgets/register_step_profile.dart';
import '../widgets/register_sticky_footer.dart';

class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  static const Map<String, String> _knownUniversityDomains = {
    'puccampinas.edu.br': 'PUC-Campinas',
    'puc-campinas.edu.br': 'PUC-Campinas',
    'unicamp.br': 'UNICAMP',
    'dac.unicamp.br': 'UNICAMP',
    'usp.br': 'USP',
    'unesp.br': 'UNESP',
    'mackenzie.br': 'Mackenzie',
  };

  final _formKey = GlobalKey<FormState>();

  // Estado único centralizado para as 3 etapas
  final _emailController = TextEditingController();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _universityController = TextEditingController();
  final _courseController = TextEditingController();
  final _enrollmentController = TextEditingController();
  final _passwordController = TextEditingController();

  final _emailFocusNode = FocusNode();
  final _nameFocusNode = FocusNode();
  final _phoneFocusNode = FocusNode();
  final _universityFocusNode = FocusNode();
  final _courseFocusNode = FocusNode();
  final _enrollmentFocusNode = FocusNode();
  final _passwordFocusNode = FocusNode();

  int _currentStep = 1;
  bool _isMovingForward = true;
  bool _obscurePassword = true;
  String? _autoDetectedUniversity;

  @override
  void initState() {
    super.initState();
    _emailController.addListener(_onEmailChanged);
    _nameController.addListener(_onFormChanged);
    _phoneController.addListener(_onFormChanged);
    _universityController.addListener(_onFormChanged);
    _courseController.addListener(_onFormChanged);
    _enrollmentController.addListener(_onFormChanged);
    _passwordController.addListener(_onFormChanged);
  }

  @override
  void dispose() {
    _emailController.dispose();
    _nameController.dispose();
    _phoneController.dispose();
    _universityController.dispose();
    _courseController.dispose();
    _enrollmentController.dispose();
    _passwordController.dispose();

    _emailFocusNode.dispose();
    _nameFocusNode.dispose();
    _phoneFocusNode.dispose();
    _universityFocusNode.dispose();
    _courseFocusNode.dispose();
    _enrollmentFocusNode.dispose();
    _passwordFocusNode.dispose();
    super.dispose();
  }

  void _onFormChanged() {
    if (mounted) setState(() {});
  }

  void _onEmailChanged() {
    final email = _emailController.text.trim().toLowerCase();
    String? detected;
    final atIndex = email.indexOf('@');
    if (atIndex != -1 && atIndex < email.length - 1) {
      final domain = email.substring(atIndex + 1);
      for (final entry in _knownUniversityDomains.entries) {
        if (domain == entry.key || domain.endsWith('.${entry.key}')) {
          detected = entry.value;
          break;
        }
      }
    }

    if (detected != null) {
      final currentUni = _universityController.text.trim();
      if (currentUni.isEmpty || currentUni == _autoDetectedUniversity) {
        _universityController.text = detected;
      }
      _autoDetectedUniversity = detected;
    } else if (_autoDetectedUniversity != null) {
      if (_universityController.text.trim() == _autoDetectedUniversity) {
        _universityController.clear();
      }
      _autoDetectedUniversity = null;
    }

    if (mounted) setState(() {});
  }

  void _handleDomainChipSelected(String domain) {
    final text = _emailController.text.trim();
    final atIndex = text.indexOf('@');
    final localPart = atIndex != -1 ? text.substring(0, atIndex) : text;
    final updated = '$localPart$domain';

    _emailController.value = TextEditingValue(
      text: updated,
      selection: TextSelection.collapsed(
        offset: localPart.isEmpty ? 0 : updated.length,
      ),
    );
    _emailFocusNode.requestFocus();
  }

  bool get _isUniversityIdentifiedByEmail =>
      _autoDetectedUniversity != null &&
      _universityController.text.trim().toLowerCase() ==
          _autoDetectedUniversity!.toLowerCase();

  bool get _isStep1Valid => Validators.email(_emailController.text) == null;

  bool get _isStep2Valid =>
      Validators.name(_nameController.text) == null &&
      Validators.phone(_phoneController.text) == null;

  bool get _isStep3Valid =>
      Validators.required(_universityController.text, field: 'Universidade') ==
          null &&
      Validators.required(_courseController.text, field: 'Curso') == null &&
      Validators.required(_enrollmentController.text, field: 'Matrícula') ==
          null &&
      RegisterStepAcademicSecurity.isPasswordStrong(_passwordController.text);

  bool get _isCurrentStepValid => switch (_currentStep) {
    1 => _isStep1Valid,
    2 => _isStep2Valid,
    3 => _isStep3Valid,
    _ => false,
  };

  void _handleBack() {
    if (ref.read(authActionProvider).isLoading) return;
    if (_currentStep > 1) {
      setState(() {
        _isMovingForward = false;
        _currentStep--;
      });
    } else {
      context.go(AppRoutes.login);
    }
  }

  Future<void> _handleNextOrSubmit() async {
    if (ref.read(authActionProvider).isLoading) return;
    if (!_isCurrentStepValid) {
      _formKey.currentState?.validate();
      return;
    }

    if (_currentStep < 3) {
      setState(() {
        _isMovingForward = true;
        _currentStep++;
      });
      return;
    }

    await _submit();
  }

  Future<void> _submit() async {
    if (!_isStep3Valid) return;

    final input = RegisterInput(
      email: _emailController.text.trim(),
      password: _passwordController.text,
      profile: ProfileInput(
        name: _nameController.text.trim(),
        phone: _phoneController.text.trim(),
        university: _universityController.text.trim(),
        course: _courseController.text.trim(),
        enrollmentNumber: _enrollmentController.text.trim(),
      ),
    );

    await ref.read(authActionProvider.notifier).register(input);
  }

  Widget _buildStepContent(bool isLoading) {
    return switch (_currentStep) {
      1 => RegisterStepEmail(
        key: const ValueKey<int>(1),
        emailController: _emailController,
        emailFocusNode: _emailFocusNode,
        isLoading: isLoading,
        onDomainChipSelected: _handleDomainChipSelected,
        onSubmitStep: _handleNextOrSubmit,
      ),
      2 => RegisterStepProfile(
        key: const ValueKey<int>(2),
        nameController: _nameController,
        phoneController: _phoneController,
        nameFocusNode: _nameFocusNode,
        phoneFocusNode: _phoneFocusNode,
        isLoading: isLoading,
        onSubmitStep: _handleNextOrSubmit,
      ),
      _ => RegisterStepAcademicSecurity(
        key: const ValueKey<int>(3),
        universityController: _universityController,
        courseController: _courseController,
        enrollmentController: _enrollmentController,
        passwordController: _passwordController,
        universityFocusNode: _universityFocusNode,
        courseFocusNode: _courseFocusNode,
        enrollmentFocusNode: _enrollmentFocusNode,
        passwordFocusNode: _passwordFocusNode,
        isUniversityIdentifiedByEmail: _isUniversityIdentifiedByEmail,
        obscurePassword: _obscurePassword,
        isLoading: isLoading,
        onTogglePasswordVisibility: () {
          setState(() {
            _obscurePassword = !_obscurePassword;
          });
        },
        onSubmitStep: _handleNextOrSubmit,
      ),
    };
  }

  @override
  Widget build(BuildContext context) {
    final actionState = ref.watch(authActionProvider);
    final isLoading = actionState.isLoading;

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _handleBack();
      },
      child: Scaffold(
        backgroundColor: OnboardingDesignTokens.background,
        resizeToAvoidBottomInset: true,
        body: SafeArea(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              RegisterShellHeader(
                currentStep: _currentStep,
                totalSteps: 3,
                onBack: isLoading ? null : _handleBack,
              ),
              Expanded(
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 440),
                    child: Form(
                      key: _formKey,
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            if (actionState.hasError) ...[
                              Container(
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFFEF2F2),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(
                                    color: const Color(0xFFFECACA),
                                  ),
                                ),
                                child: Text(
                                  authErrorMessage(actionState.error!),
                                  style: const TextStyle(
                                    color: Color(0xFFB91C1C),
                                    fontSize: 14,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 20),
                            ],
                            ClipRect(
                              child: AnimatedSwitcher(
                                duration: const Duration(milliseconds: 250),
                                switchInCurve: Curves.easeOut,
                                switchOutCurve: Curves.easeOut,
                                layoutBuilder:
                                    (currentChild, previousChildren) {
                                      return Stack(
                                        alignment: Alignment.topLeft,
                                        children: <Widget>[
                                          ...previousChildren,
                                          ?currentChild,
                                        ],
                                      );
                                    },
                                transitionBuilder: (child, animation) {
                                  final isIncoming =
                                      child.key == ValueKey<int>(_currentStep);
                                  final beginOffset = _isMovingForward
                                      ? (isIncoming
                                            ? const Offset(1.0, 0.0)
                                            : const Offset(-1.0, 0.0))
                                      : (isIncoming
                                            ? const Offset(-1.0, 0.0)
                                            : const Offset(1.0, 0.0));

                                  return SlideTransition(
                                    position: Tween<Offset>(
                                      begin: beginOffset,
                                      end: Offset.zero,
                                    ).animate(animation),
                                    child: FadeTransition(
                                      opacity: animation,
                                      child: child,
                                    ),
                                  );
                                },
                                child: _buildStepContent(isLoading),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              RegisterStickyFooter(
                label: _currentStep < 3 ? buildActionButtonLabel('Continuar') : 'Criar Conta',
                isEnabled: _isCurrentStepValid,
                isLoading: isLoading,
                showLoginPrompt: _currentStep == 1,
                onPrimaryPressed: _handleNextOrSubmit,
                onLoginPressed: () => context.go(AppRoutes.login),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
