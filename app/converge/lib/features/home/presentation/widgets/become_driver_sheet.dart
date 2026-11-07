// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/cnh_input_formatter.dart';
import '../../../../core/utils/cpf_input_formatter.dart';
import '../../../../core/utils/email_input_formatter.dart';
import '../../../../core/utils/phone_input_formatter.dart';
import '../../../auth/data/models/driver_license_input.dart';
import '../../../auth/data/models/pix_key_type.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/become_driver_controller.dart';
import '../../../auth/state/session/auth_session.dart';

/// Exibe o BottomSheet contextual para ativação atômica do modo motorista.
Future<bool?> showBecomeDriverSheet({
  required BuildContext context,
  VoidCallback? onSuccess,
}) {
  HapticFeedback.mediumImpact();
  return showModalBottomSheet<bool>(
    context: context,
    backgroundColor: Colors.transparent,
    barrierColor: AppColors.barrier,
    isScrollControlled: true,
    builder: (context) => BecomeDriverSheet(onSuccess: onSuccess),
  );
}

enum _DriverSetupStep { pix, license, verification, success }

/// BottomSheet com fluxo atômico em etapas:
/// 1. Configuração da chave PIX
/// 2. Dados da Carteira de Motorista (CNH)
/// 3. Simulação de aprovação e ativação via backend
/// 4. Sucesso e liberação imediata
class BecomeDriverSheet extends ConsumerStatefulWidget {
  final VoidCallback? onSuccess;

  const BecomeDriverSheet({super.key, this.onSuccess});

  @override
  ConsumerState<BecomeDriverSheet> createState() => _BecomeDriverSheetState();
}

class _BecomeDriverSheetState extends ConsumerState<BecomeDriverSheet> {
  _DriverSetupStep _currentStep = _DriverSetupStep.pix;

  // Passo 1: PIX
  final _pixFormKey = GlobalKey<FormState>();
  final _pixController = TextEditingController();
  PixKeyType _selectedPixType = PixKeyType.cpf;

  // Passo 2: CNH
  final _cnhFormKey = GlobalKey<FormState>();
  final _cnhNumberController = TextEditingController();
  DriverLicenseCategory _selectedCategory = DriverLicenseCategory.b;
  DateTime _expirationDate = DateTime.now().add(const Duration(days: 365 * 3));

  // Passo 3: Simulação de aprovação
  String _simulationMessage = 'Iniciando validação cadastral...';
  double _simulationProgress = 0.2;
  String? _errorMessage;
  bool _isValidatingCpf = false;

  @override
  void initState() {
    super.initState();
    // Pré-carrega valores caso o usuário já tenha cadastrado anteriormente
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final session = ref.read(authControllerProvider).value;
      if (session is Authenticated) {
        if (session.user.pixKey != null) {
          _pixController.text = session.user.pixKey!;
        }
        if (session.user.pixKeyType != null) {
          setState(() => _selectedPixType = session.user.pixKeyType!);
        }
      }
    });
  }

  @override
  void dispose() {
    _pixController.dispose();
    _cnhNumberController.dispose();
    super.dispose();
  }

  Future<void> _onNextToCnh() async {
    if (!_pixFormKey.currentState!.validate()) return;
    if (_isValidatingCpf) return;

    if (_selectedPixType == PixKeyType.cpf) {
      setState(() {
        _isValidatingCpf = true;
        _errorMessage = null;
      });
      try {
        final result = await ref
            .read(becomeDriverProvider.notifier)
            .validateCpf(_pixController.text);
        if (!mounted) return;
        if (!result.valid) {
          setState(() {
            _isValidatingCpf = false;
            _errorMessage = result.message.isNotEmpty
                ? result.message
                : 'CPF informado é inválido.';
          });
          return;
        }
      } catch (e) {
        if (!mounted) return;
        setState(() {
          _isValidatingCpf = false;
          _errorMessage = authErrorMessage(e);
        });
        return;
      } finally {
        if (mounted) setState(() => _isValidatingCpf = false);
      }
    }

    HapticFeedback.lightImpact();
    setState(() {
      _currentStep = _DriverSetupStep.license;
      _errorMessage = null;
    });
  }

  Future<void> _startAtomicActivation() async {
    if (!_cnhFormKey.currentState!.validate()) return;
    HapticFeedback.mediumImpact();

    setState(() {
      _currentStep = _DriverSetupStep.verification;
      _simulationProgress = 0.25;
      _simulationMessage = 'Conectando ao sistema e salvando chave PIX...';
      _errorMessage = null;
    });

    final licenseInput = DriverLicenseInput(
      number: _cnhNumberController.text.trim(),
      category: _selectedCategory,
      expiresAt: _expirationDate,
    );

    // Etapas da simulação visual enquanto a requisição atômica ocorre
    Future.delayed(const Duration(milliseconds: 700), () {
      if (mounted && _currentStep == _DriverSetupStep.verification) {
        setState(() {
          _simulationProgress = 0.60;
          _simulationMessage =
              'Analisando autenticidade e validade da CNH (${_selectedCategory.label})...';
        });
      }
    });

    Future.delayed(const Duration(milliseconds: 1400), () {
      if (mounted && _currentStep == _DriverSetupStep.verification) {
        setState(() {
          _simulationProgress = 0.90;
          _simulationMessage =
              'Aprovando documento e habilitando modo motorista...';
        });
      }
    });

    try {
      await ref
          .read(becomeDriverProvider.notifier)
          .activateDriverAtomic(
            pixKey: _pixController.text.trim(),
            pixKeyType: _selectedPixType,
            license: licenseInput,
            verificationDelay: const Duration(milliseconds: 2000),
          );

      if (!mounted) return;

      HapticFeedback.mediumImpact();
      setState(() {
        _currentStep = _DriverSetupStep.success;
        _simulationProgress = 1.0;
      });
      widget.onSuccess?.call();
    } catch (e, st) {
      debugPrint('[BecomeDriverSheet] Falha na ativação do motorista: $e\n$st');
      if (!mounted) return;
      final msg = authErrorMessage(e);
      setState(() {
        _errorMessage = msg;
        _currentStep = _DriverSetupStep.license;
      });
    }
  }

  Future<void> _selectExpirationDate() async {
    HapticFeedback.lightImpact();
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _expirationDate.isAfter(now)
          ? _expirationDate
          : now.add(const Duration(days: 365)),
      firstDate: now.add(const Duration(days: 1)),
      lastDate: now.add(const Duration(days: 365 * 10)),
      builder: (context, child) {
        return Theme(
          data: ThemeData.light().copyWith(
            colorScheme: const ColorScheme.light(
              primary: AppColors.primary,
              onPrimary: AppColors.dockItemActive,
              surface: AppColors.cardBackground,
              onSurface: AppColors.textPrimary,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null && mounted) {
      setState(() => _expirationDate = picked);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomPadding = MediaQuery.paddingOf(context).bottom;

    return AnimatedPadding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      duration: const Duration(milliseconds: 200),
      curve: Curves.easeOut,
      child: Container(
        decoration: BoxDecoration(
          color: AppColors.cardBackground,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
          border: Border.all(color: AppColors.borderSubtle),
        ),
        padding: EdgeInsets.fromLTRB(22, 12, 22, 24 + bottomPadding),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Alça superior
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.borderMedium,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // Cabeçalho e Indicador de Etapas
              _buildHeader(),
              const SizedBox(height: 20),

              if (_errorMessage != null) ...[
                _buildErrorBanner(_errorMessage!),
                const SizedBox(height: 16),
              ],

              // Conteúdo dinâmico de cada etapa
              switch (_currentStep) {
                _DriverSetupStep.pix => _buildPixStep(),
                _DriverSetupStep.license => _buildLicenseStep(),
                _DriverSetupStep.verification => _buildVerificationStep(),
                _DriverSetupStep.success => _buildSuccessStep(),
              },
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(
                Icons.directions_car_rounded,
                color: AppColors.dockItemActive,
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Ativação do Modo Motorista',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                      letterSpacing: -0.3,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _stepSubtitle(),
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),

        // Stepper Visual
        Row(
          children: [
            _buildStepIndicator(
              label: '1. Chave PIX',
              isActive: _currentStep == _DriverSetupStep.pix,
              isCompleted: _currentStep != _DriverSetupStep.pix,
            ),
            const SizedBox(width: 8),
            _buildStepIndicator(
              label: '2. CNH',
              isActive: _currentStep == _DriverSetupStep.license,
              isCompleted:
                  _currentStep == _DriverSetupStep.verification ||
                  _currentStep == _DriverSetupStep.success,
            ),
            const SizedBox(width: 8),
            _buildStepIndicator(
              label: '3. Aprovação',
              isActive: _currentStep == _DriverSetupStep.verification,
              isCompleted: _currentStep == _DriverSetupStep.success,
            ),
          ],
        ),
      ],
    );
  }

  String _stepSubtitle() => switch (_currentStep) {
    _DriverSetupStep.pix => 'Passo 1 de 2: Cadastre sua chave de rateio.',
    _DriverSetupStep.license =>
      'Passo 2 de 2: Informe os dados da habilitação.',
    _DriverSetupStep.verification => 'Processando verificação de segurança...',
    _DriverSetupStep.success => 'Perfil habilitado com sucesso!',
  };

  Widget _buildStepIndicator({
    required String label,
    required bool isActive,
    required bool isCompleted,
  }) {
    final bgColor = isCompleted
        ? AppColors.success
        : (isActive ? AppColors.primary : AppColors.neutralLight);
    final textColor = isCompleted || isActive
        ? AppColors.dockItemActive
        : AppColors.textMuted;

    return Expanded(
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(vertical: 6),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Center(
          child: Text(
            label,
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: textColor,
            ),
          ),
        ),
      ),
    );
  }

  // --- PASSO 1: CHAVE PIX ---
  Widget _buildPixStep() {
    return Form(
      key: _pixFormKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.neutralLight,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.borderSubtle),
            ),
            child: const Row(
              children: [
                Icon(
                  Icons.payments_rounded,
                  color: AppColors.textPrimary,
                  size: 22,
                ),
                SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Sua chave PIX é necessária para que os passageiros façam o rateio das caronas diretamente para você.',
                    style: TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textDark,
                      height: 1.3,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          const Text(
            'Tipo de Chave PIX',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),

          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: PixKeyType.values.map((type) {
              final isSelected = _selectedPixType == type;
              return ChoiceChip(
                label: Text(type.label),
                selected: isSelected,
                selectedColor: AppColors.primary,
                backgroundColor: AppColors.chipBackground,
                labelStyle: TextStyle(
                  color: isSelected
                      ? AppColors.dockItemActive
                      : AppColors.textPrimary,
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                  fontSize: 12.5,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                  side: BorderSide(
                    color: isSelected
                        ? AppColors.primary
                        : AppColors.chipBorder,
                  ),
                ),
                showCheckmark: false,
                onSelected: (val) {
                  if (val) {
                    HapticFeedback.selectionClick();
                    setState(() {
                      _selectedPixType = type;
                      _pixController.clear();
                      _errorMessage = null;
                    });
                  }
                },
              );
            }).toList(),
          ),

          const SizedBox(height: 16),

          TextFormField(
            controller: _pixController,
            decoration: InputDecoration(
              labelText: 'Chave PIX (${_selectedPixType.label})',
              hintText: _pixHintText(),
              prefixIcon: const Icon(
                Icons.account_balance_wallet_rounded,
                size: 20,
                color: AppColors.textSecondary,
              ),
              filled: true,
              fillColor: AppColors.inputBackground,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.borderSubtle),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.borderSubtle),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(
                  color: AppColors.primary,
                  width: 1.5,
                ),
              ),
            ),
            keyboardType: _selectedPixType == PixKeyType.email
                ? TextInputType.emailAddress
                : (_selectedPixType == PixKeyType.phone ||
                          _selectedPixType == PixKeyType.cpf
                      ? TextInputType.number
                      : TextInputType.text),
            inputFormatters: switch (_selectedPixType) {
              PixKeyType.cpf => const [CpfInputFormatter()],
              PixKeyType.phone => const [PhoneInputFormatter()],
              PixKeyType.email => const [EmailInputFormatter()],
              PixKeyType.random => const [],
            },
            validator: (value) {
              final val = value?.trim() ?? '';
              if (val.isEmpty) return 'Informe sua chave PIX';
              switch (_selectedPixType) {
                case PixKeyType.cpf:
                  final digits = val.replaceAll(RegExp(r'\D'), '');
                  if (digits.length != 11) return 'CPF deve conter 11 dígitos';
                case PixKeyType.phone:
                  final digits = val.replaceAll(RegExp(r'\D'), '');
                  if (digits.length < 10 || digits.length > 11) {
                    return 'Telefone deve ter DDD + número';
                  }
                case PixKeyType.email:
                  if (!val.contains('@') || !val.contains('.')) {
                    return 'E-mail inválido';
                  }
                case PixKeyType.random:
                  if (val.length < 16) return 'Chave aleatória inválida';
              }
              return null;
            },
          ),

          const SizedBox(height: 24),

          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: _isValidatingCpf ? null : _onNextToCnh,
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: AppColors.dockItemActive,
                disabledBackgroundColor: AppColors.primary,
                disabledForegroundColor: AppColors.dockItemActive,
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: _isValidatingCpf
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: AppColors.dockItemActive,
                      ),
                    )
                  : const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Avançar para CNH',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Icon(Icons.arrow_forward_rounded, size: 20),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  String _pixHintText() => switch (_selectedPixType) {
    PixKeyType.cpf => '000.000.000-00',
    PixKeyType.email => 'seu.email@exemplo.com',
    PixKeyType.phone => '(19) 99999-9999',
    PixKeyType.random => 'Chave aleatória gerada pelo banco',
  };

  // --- PASSO 2: CARTEIRA DE MOTORISTA ---
  Widget _buildLicenseStep() {
    return Form(
      key: _cnhFormKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.neutralLight,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.borderSubtle),
            ),
            child: const Row(
              children: [
                Icon(
                  Icons.badge_rounded,
                  color: AppColors.textPrimary,
                  size: 22,
                ),
                SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Para segurança de todos no campus, exigimos uma CNH válida e ativa.',
                    style: TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textDark,
                      height: 1.3,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          TextFormField(
            controller: _cnhNumberController,
            decoration: InputDecoration(
              labelText: 'Número do Registro da CNH',
              hintText: '11 dígitos numéricos',
              prefixIcon: const Icon(
                Icons.pin_rounded,
                size: 20,
                color: AppColors.textSecondary,
              ),
              filled: true,
              fillColor: AppColors.inputBackground,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.borderSubtle),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.borderSubtle),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(
                  color: AppColors.primary,
                  width: 1.5,
                ),
              ),
            ),
            keyboardType: TextInputType.number,
            inputFormatters: const [CnhInputFormatter()],
            validator: (value) {
              final val = value?.trim() ?? '';
              if (val.isEmpty) return 'Informe o número da CNH';
              if (val.length != 11) {
                return 'A CNH deve ter exatamente 11 dígitos numéricos';
              }
              return null;
            },
          ),

          const SizedBox(height: 16),

          const Text(
            'Categoria da Habilitação',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),

          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: DriverLicenseCategory.values.map((cat) {
              final isSelected = _selectedCategory == cat;
              return ChoiceChip(
                label: Text('Categoria ${cat.label}'),
                selected: isSelected,
                selectedColor: AppColors.primary,
                backgroundColor: AppColors.chipBackground,
                labelStyle: TextStyle(
                  color: isSelected
                      ? AppColors.dockItemActive
                      : AppColors.textPrimary,
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                  fontSize: 12.5,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                  side: BorderSide(
                    color: isSelected
                        ? AppColors.primary
                        : AppColors.chipBorder,
                  ),
                ),
                showCheckmark: false,
                onSelected: (val) {
                  if (val) {
                    HapticFeedback.selectionClick();
                    setState(() => _selectedCategory = cat);
                  }
                },
              );
            }).toList(),
          ),

          const SizedBox(height: 16),

          // Seletor de Validade da CNH
          InkWell(
            onTap: _selectExpirationDate,
            borderRadius: BorderRadius.circular(12),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
              decoration: BoxDecoration(
                color: AppColors.inputBackground,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.calendar_today_rounded,
                    size: 20,
                    color: AppColors.textSecondary,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Validade da CNH',
                          style: TextStyle(
                            fontSize: 11.5,
                            color: AppColors.textSecondary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '${_expirationDate.day.toString().padLeft(2, '0')}/${_expirationDate.month.toString().padLeft(2, '0')}/${_expirationDate.year}',
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Text(
                    'Alterar',
                    style: TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 24),

          Row(
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: AppColors.neutralLight,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.borderSubtle),
                ),
                child: IconButton(
                  onPressed: () {
                    HapticFeedback.lightImpact();
                    setState(() => _currentStep = _DriverSetupStep.pix);
                  },
                  icon: const Icon(
                    Icons.arrow_back_rounded,
                    color: AppColors.textPrimary,
                    size: 22,
                  ),
                  tooltip: 'Voltar ao PIX',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: SizedBox(
                  height: 52,
                  child: FilledButton(
                    onPressed: _startAtomicActivation,
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: AppColors.dockItemActive,
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Validar e Ativar Modo',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Icon(Icons.arrow_forward_rounded, size: 20),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // --- PASSO 3: SIMULAÇÃO DE APROVAÇÃO ---
  Widget _buildVerificationStep() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: Column(
        children: [
          Container(
            width: 72,
            height: 72,
            decoration: BoxDecoration(
              color: AppColors.neutralLight,
              shape: BoxShape.circle,
              border: Border.all(color: AppColors.borderSubtle, width: 2),
            ),
            child: const Center(
              child: SizedBox(
                width: 36,
                height: 36,
                child: CircularProgressIndicator(
                  strokeWidth: 3.2,
                  valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
                ),
              ),
            ),
          ),
          const SizedBox(height: 20),

          const Text(
            'Validação de Habilitação',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
              letterSpacing: -0.2,
            ),
          ),
          const SizedBox(height: 8),

          Text(
            _simulationMessage,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 24),

          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: _simulationProgress,
              minHeight: 6,
              backgroundColor: AppColors.progressTrack,
              valueColor: const AlwaysStoppedAnimation<Color>(
                AppColors.primary,
              ),
            ),
          ),
          const SizedBox(height: 16),

          const Text(
            'Realizando transição atômica de perfil universitário...',
            style: TextStyle(
              fontSize: 11.5,
              color: AppColors.textMuted,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  // --- PASSO 4: SUCESSO ---
  Widget _buildSuccessStep() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Center(
            child: Container(
              width: 64,
              height: 64,
              decoration: const BoxDecoration(
                color: AppColors.successBackground,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.check_circle_rounded,
                color: AppColors.success,
                size: 40,
              ),
            ),
          ),
          const SizedBox(height: 16),

          const Text(
            'Modo Motorista Desbloqueado!',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 19,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
              letterSpacing: -0.3,
            ),
          ),
          const SizedBox(height: 6),

          const Text(
            'Sua chave PIX foi configurada e sua CNH foi aprovada com sucesso. Você já pode iniciar expedientes e oferecer caronas no campus.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 24),

          // Resumo dos dados cadastrados
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.neutralLight,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.borderSubtle),
            ),
            child: Column(
              children: [
                _buildSummaryRow(
                  'Chave PIX',
                  '${_pixController.text.trim()} (${_selectedPixType.label})',
                ),
                const Divider(height: 14, color: AppColors.borderSubtle),
                _buildSummaryRow('Categoria CNH', _selectedCategory.label),
                const Divider(height: 14, color: AppColors.borderSubtle),
                _buildSummaryRow(
                  'Status da Carteira',
                  'Aprovada e Válida',
                  isSuccess: true,
                ),
              ],
            ),
          ),

          const SizedBox(height: 24),

          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: () {
                HapticFeedback.lightImpact();
                Navigator.of(context).pop(true);
              },
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: AppColors.dockItemActive,
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Concluir e Começar',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                  Icon(Icons.arrow_forward_rounded, size: 20),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryRow(
    String label,
    String value, {
    bool isSuccess = false,
  }) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12.5,
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w500,
          ),
        ),
        Text(
          value,
          style: TextStyle(
            fontSize: 12.5,
            color: isSuccess ? AppColors.success : AppColors.textPrimary,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }

  Widget _buildErrorBanner(String message) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: AppColors.errorBackground,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.errorBorder),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.error_outline_rounded,
            color: AppColors.error,
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontSize: 12.5,
                color: AppColors.errorText,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
