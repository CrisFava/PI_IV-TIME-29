import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/phone_input_formatter.dart';
import '../../../../core/utils/validators.dart';
import '../../state/auth_action_controller.dart';
import '../../state/auth_controller.dart';
import '../../state/inputs/profile_input.dart';
import '../../state/session/auth_session.dart';

class CompleteProfilePage extends ConsumerStatefulWidget {
  const CompleteProfilePage({super.key});

  @override
  ConsumerState<CompleteProfilePage> createState() =>
      _CompleteProfilePageState();
}

class _CompleteProfilePageState extends ConsumerState<CompleteProfilePage> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _universityController = TextEditingController(text: 'PUC-Campinas');
  final _courseController = TextEditingController();
  final _enrollmentController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _universityController.dispose();
    _courseController.dispose();
    _enrollmentController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final profile = ProfileInput(
      name: _nameController.text.trim(),
      phone: _phoneController.text.trim(),
      university: _universityController.text.trim(),
      course: _courseController.text.trim(),
      enrollmentNumber: _enrollmentController.text.trim(),
    );

    await ref.read(authActionProvider.notifier).completeProfile(profile);
  }

  @override
  Widget build(BuildContext context) {
    final actionState = ref.watch(authActionProvider);
    final isLoading = actionState.isLoading;
    final session = ref.watch(authControllerProvider).value;
    final email = session is ProfileIncomplete ? session.authUser.email : null;

    return Scaffold(
      backgroundColor: AppColors.cardBackground,
      appBar: AppBar(
        backgroundColor: AppColors.cardBackground,
        surfaceTintColor: AppColors.cardBackground,
        title: const Text(
          'Completar cadastro',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        actions: [
          TextButton(
            onPressed: isLoading
                ? null
                : () => ref.read(authActionProvider.notifier).signOut(),
            child: const Text('Sair'),
          ),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (email != null) ...[
                      Text(
                        'Conclua seu perfil para a conta $email',
                        style: const TextStyle(color: AppColors.textBodySecondary),
                      ),
                      const SizedBox(height: 16),
                    ],
                    if (actionState.hasError) ...[
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.errorBackground,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: AppColors.errorBorder),
                        ),
                        child: Text(
                          authErrorMessage(actionState.error!),
                          style: const TextStyle(
                            color: AppColors.errorText,
                            fontSize: 14,
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                    ],
                    const Text(
                      'Nome completo',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _nameController,
                      enabled: !isLoading,
                      textCapitalization: TextCapitalization.words,
                      validator: Validators.name,
                      decoration: InputDecoration(
                        hintText: 'Ex.: João Silva',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      'Telefone com DDD',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _phoneController,
                      enabled: !isLoading,
                      keyboardType: TextInputType.phone,
                      inputFormatters: const [PhoneInputFormatter()],
                      validator: Validators.phone,
                      decoration: InputDecoration(
                        hintText: '(19) 99999-9999',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      'Universidade',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _universityController,
                      enabled: !isLoading,
                      validator: (v) =>
                          Validators.required(v, field: 'Universidade'),
                      decoration: InputDecoration(
                        hintText: 'Ex.: PUC-Campinas',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          flex: 3,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Curso',
                                style: TextStyle(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 8),
                              TextFormField(
                                controller: _courseController,
                                enabled: !isLoading,
                                validator: (v) =>
                                    Validators.required(v, field: 'Curso'),
                                decoration: InputDecoration(
                                  hintText: 'Ex.: Ciência da Computação',
                                  border: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          flex: 2,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Matrícula',
                                style: TextStyle(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 8),
                              TextFormField(
                                controller: _enrollmentController,
                                enabled: !isLoading,
                                validator: (v) =>
                                    Validators.required(v, field: 'Matrícula'),
                                decoration: InputDecoration(
                                  hintText: 'Ex.: 21004567',
                                  border: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    SizedBox(
                      height: 52,
                      child: FilledButton(
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primaryDark,
                          foregroundColor: AppColors.textLight,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        onPressed: isLoading ? null : _submit,
                        child: isLoading
                            ? const SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: AppColors.textLight,
                                ),
                              )
                            : const Text(
                                'Salvar perfil →',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
