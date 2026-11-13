// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/phone_input_formatter.dart';
import '../../../../core/utils/validators.dart';
import '../../data/models/pix_key_type.dart';
import '../../data/models/user_model.dart';
import '../../state/auth_action_controller.dart';
import '../../state/auth_controller.dart';
import '../../../vehicle/presentation/widgets/vehicle_input_decoration.dart';

/// Abre a edição do perfil. Devolve true quando os dados foram salvos.
Future<bool?> showEditProfileSheet(BuildContext context, UserModel user) {
  HapticFeedback.lightImpact();
  return showModalBottomSheet<bool>(
    context: context,
    backgroundColor: AppColors.cardBackground,
    barrierColor: AppColors.barrier,
    isScrollControlled: true,
    useSafeArea: true,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
    ),
    builder: (_) => EditProfileSheet(user: user),
  );
}

/// Edição de nome, telefone e chave PIX do usuário (`PUT /users/me`).
class EditProfileSheet extends ConsumerStatefulWidget {
  final UserModel user;

  const EditProfileSheet({super.key, required this.user});

  @override
  ConsumerState<EditProfileSheet> createState() => _EditProfileSheetState();
}

class _EditProfileSheetState extends ConsumerState<EditProfileSheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name;
  late final TextEditingController _phone;
  late final TextEditingController _pixKey;
  late PixKeyType _pixType;
  bool _isSaving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    final user = widget.user;
    _name = TextEditingController(text: user.name);
    _phone = TextEditingController(
      text: const PhoneInputFormatter()
          .formatEditUpdate(
            TextEditingValue.empty,
            TextEditingValue(text: user.phone),
          )
          .text,
    );
    _pixKey = TextEditingController(text: user.pixKey ?? '');
    _pixType = user.pixKeyType ?? PixKeyType.cpf;
  }

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _pixKey.dispose();
    super.dispose();
  }

  String? _validatePix(String? value) {
    final key = value?.trim() ?? '';
    // Motorista precisa de chave PIX para receber o rateio
    if (key.isEmpty) {
      if (widget.user.isDriver) return 'Motoristas precisam de uma chave PIX.';
      // Ainda não dá para remover a chave: salvar vazio manteria a atual
      if (widget.user.pixKey?.trim().isNotEmpty ?? false) {
        return 'Ainda não é possível remover a chave PIX. A chave atual será mantida.';
      }
      return null;
    }
    final digits = key.replaceAll(RegExp(r'\D'), '');
    return switch (_pixType) {
      PixKeyType.cpf when digits.length != 11 => 'CPF deve conter 11 dígitos',
      PixKeyType.phone when digits.length < 10 || digits.length > 11 =>
        'Telefone deve ter DDD + número',
      PixKeyType.email when !key.contains('@') || !key.contains('.') =>
        'E-mail inválido',
      PixKeyType.random when key.length < 16 => 'Chave aleatória inválida',
      _ => null,
    };
  }

  Future<void> _save() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) {
      HapticFeedback.heavyImpact();
      return;
    }

    setState(() {
      _isSaving = true;
      _error = null;
    });
    try {
      await ref
          .read(authControllerProvider.notifier)
          .updateProfile(
            name: _name.text,
            phone: _phone.text,
            pixKey: _pixKey.text,
            pixKeyType: _pixType,
          );
      if (!mounted) return;
      HapticFeedback.mediumImpact();
      Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      HapticFeedback.heavyImpact();
      setState(() {
        _isSaving = false;
        _error = authErrorMessage(e);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    final bottomPadding = MediaQuery.paddingOf(context).bottom;

    return Padding(
      padding: EdgeInsets.only(bottom: bottomInset),
      child: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(22, 12, 22, 24 + bottomPadding),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
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
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      'Editar perfil',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                        letterSpacing: -0.3,
                      ),
                    ),
                  ),
                  IconButton(
                    tooltip: 'Fechar',
                    onPressed: _isSaving
                        ? null
                        : () => Navigator.of(context).pop(false),
                    icon: const Icon(
                      Icons.close_rounded,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              if (_error != null) ...[
                Text(
                  _error!,
                  style: const TextStyle(
                    fontSize: 12.5,
                    color: AppColors.errorText,
                  ),
                ),
                const SizedBox(height: 12),
              ],
              TextFormField(
                controller: _name,
                enabled: !_isSaving,
                textCapitalization: TextCapitalization.words,
                decoration: vehicleInputDecoration(
                  label: 'Nome',
                  icon: Icons.person_rounded,
                ),
                validator: Validators.name,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _phone,
                enabled: !_isSaving,
                keyboardType: TextInputType.phone,
                inputFormatters: const [PhoneInputFormatter()],
                decoration: vehicleInputDecoration(
                  label: 'Telefone',
                  hint: '(19) 99999-9999',
                  icon: Icons.phone_rounded,
                ),
                validator: Validators.phone,
              ),
              const SizedBox(height: 16),
              const Text(
                'Chave PIX (para receber o rateio)',
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
                children: [
                  for (final type in PixKeyType.values)
                    ChoiceChip(
                      label: Text(type.label),
                      selected: type == _pixType,
                      selectedColor: AppColors.primary,
                      backgroundColor: AppColors.chipBackground,
                      labelStyle: TextStyle(
                        color: type == _pixType
                            ? AppColors.dockItemActive
                            : AppColors.textPrimary,
                        fontWeight: type == _pixType
                            ? FontWeight.w700
                            : FontWeight.w500,
                        fontSize: 12.5,
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                        side: BorderSide(
                          color: type == _pixType
                              ? AppColors.primary
                              : AppColors.chipBorder,
                        ),
                      ),
                      showCheckmark: false,
                      onSelected: _isSaving
                          ? null
                          : (selected) {
                              if (!selected) return;
                              HapticFeedback.selectionClick();
                              setState(() => _pixType = type);
                            },
                    ),
                ],
              ),
              const SizedBox(height: 10),
              TextFormField(
                controller: _pixKey,
                enabled: !_isSaving,
                decoration: vehicleInputDecoration(
                  label: 'Chave PIX',
                  icon: Icons.pix_rounded,
                ),
                validator: _validatePix,
              ),
              const SizedBox(height: 22),
              SizedBox(
                height: 52,
                child: FilledButton(
                  key: const ValueKey('save_profile_button'),
                  onPressed: _isSaving ? null : _save,
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: AppColors.dockItemActive,
                    disabledBackgroundColor: AppColors.borderMedium,
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: _isSaving
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.2,
                            color: AppColors.dockItemActive,
                          ),
                        )
                      : const Text(
                          'Salvar alterações',
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
    );
  }
}
