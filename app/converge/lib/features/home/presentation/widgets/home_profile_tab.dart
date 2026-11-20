// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app_routes.dart';
import '../../../../core/config/styles.dart';
import '../../../auth/data/models/user_model.dart';
import '../../../auth/presentation/widgets/edit_profile_sheet.dart';
import '../../../auth/presentation/widgets/profile_photo_avatar.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/session/auth_session.dart';
import '../../../indicator/presentation/widgets/my_impact_card.dart';
import '../../../vehicle/presentation/widgets/my_vehicles_section.dart';
import '../../../vehicle/state/driver_access_provider.dart';

class HomeProfileTab extends ConsumerWidget {
  final UserModel user;
  final double bottomPadding;

  const HomeProfileTab({
    super.key,
    required this.user,
    required this.bottomPadding,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isSigningOut = ref.watch(authActionProvider).isLoading;

    return ListView(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 120 + bottomPadding),
      children: [
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Column(
            children: [
              ProfilePhotoAvatar(user: user),
              const SizedBox(height: 12),
              Text(
                user.name.toString(),
                style: const TextStyle(
                  fontSize: 19,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                  letterSpacing: -0.3,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                user.email.toString(),
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 14),

              // Status das Permissões do Usuário (Passenger e Driver)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.neutralLight,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _buildRoleBadge('Passageiro', true),
                    _buildRoleBadge(
                      ref.watch(driverAccessProvider).isBlockedByVehicle
                          ? 'Motorista (sem carro)'
                          : 'Motorista',
                      ref.watch(driverAccessProvider).canDrive,
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),

              const Divider(color: AppColors.neutralLight),
              const SizedBox(height: 10),
              _buildProfileRow('Telefone', user.phone.toString()),
              _buildProfileRow(
                'Universidade',
                user.academicRecord.university.toString(),
              ),
              _buildProfileRow('Curso', user.academicRecord.course.toString()),
              _buildProfileRow(
                'Matrícula',
                user.academicRecord.enrollmentNumber.toString(),
              ),
              if (user.pixKey != null)
                _buildProfileRow('Chave PIX', user.pixKey.toString()),

              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  key: const ValueKey('edit_profile_button'),
                  onPressed: () => _handleEditProfile(context, ref),
                  icon: const Icon(
                    Icons.edit_rounded,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
                  label: const Text(
                    'Editar perfil',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    side: const BorderSide(color: AppColors.borderMedium),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  key: const ValueKey('blocked_users_button'),
                  onPressed: () => context.push(AppRoutes.blockedUsers),
                  icon: const Icon(
                    Icons.block_rounded,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
                  label: const Text(
                    'Usuários bloqueados',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    side: const BorderSide(color: AppColors.borderMedium),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        // Meu impacto: abre os indicadores de mobilidade e ambientais
        const SizedBox(height: 16),
        MyImpactCard(onOpen: () => context.push(AppRoutes.indicators)),
        // Motorista: carros cadastrados (sem nenhum, o acesso fica bloqueado)
        if (user.isDriver) ...[
          const SizedBox(height: 16),
          const MyVehiclesSection(),
        ],
        const SizedBox(height: 24),
        // Sair da Conta fica no fim da tela, longe do que se usa todo dia
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed:
                isSigningOut ? null : () => _confirmSignOut(context, ref),
            icon: isSigningOut
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.error,
                    ),
                  )
                : const Icon(
                    Icons.logout_rounded,
                    size: 18,
                    color: AppColors.error,
                  ),
            label: Text(
              isSigningOut ? 'Saindo...' : 'Sair da Conta',
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: AppColors.error,
              ),
            ),
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 14),
              side: const BorderSide(color: AppColors.errorBorder),
              backgroundColor: AppColors.errorBackground,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _handleEditProfile(BuildContext context, WidgetRef ref) async {
    final session = ref.read(authControllerProvider).value;
    if (session is! Authenticated) return;
    await showEditProfileSheet(context, session.user);
  }

  Future<void> _confirmSignOut(BuildContext context, WidgetRef ref) async {
    if (ref.read(authActionProvider).isLoading) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardBackground,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Row(
          children: [
            Icon(Icons.logout_rounded, color: AppColors.error, size: 24),
            SizedBox(width: 10),
            Text(
              'Sair da Conta',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
              ),
            ),
          ],
        ),
        content: const Text(
          'Deseja realmente sair da sua conta Converge?',
          style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text(
              'Cancelar',
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.error,
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text(
              'Sim, Sair',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.textLight,
              ),
            ),
          ),
        ],
      ),
    );

    if (confirmed == true && context.mounted) {
      final success = await ref.read(authActionProvider.notifier).signOut();
      if (!success && context.mounted) {
        final error = ref.read(authActionProvider).error;
        final message = error != null
            ? authErrorMessage(error)
            : 'Erro ao sair da conta. Tente novamente.';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(message),
            backgroundColor: AppColors.error,
          ),
        );
      }
    }
  }

  Widget _buildRoleBadge(String label, bool isEnabled) {
    return Row(
      children: [
        Icon(
          isEnabled ? Icons.check_circle_rounded : Icons.lock_rounded,
          size: 16,
          color: isEnabled ? AppColors.success : AppColors.textMuted,
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: isEnabled ? FontWeight.w700 : FontWeight.w500,
            color: isEnabled ? AppColors.textPrimary : AppColors.textMuted,
          ),
        ),
      ],
    );
  }

  Widget _buildProfileRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w500,
            ),
          ),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: const TextStyle(
                fontSize: 13.5,
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
