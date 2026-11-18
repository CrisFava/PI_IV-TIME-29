// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/show_app_notice.dart';
import 'block_confirm_dialog.dart';
import 'report_sheet.dart';

enum _SafetyAction { report, block }

/// Menu "Denunciar / Bloquear" para o outro usuário que aparece numa tela.
///
/// Usado nos cartões de pedido e de motorista e no chat, para o comportamento ser o mesmo.
class UserSafetyMenu extends StatelessWidget {
  final String userId;
  final String userName;

  /// Viagem relacionada à denúncia, quando houver.
  final String? rideId;

  const UserSafetyMenu({
    super.key,
    required this.userId,
    required this.userName,
    this.rideId,
  });

  Future<void> _onSelected(BuildContext context, _SafetyAction action) async {
    switch (action) {
      case _SafetyAction.report:
        final sent = await showReportSheet(
          context,
          reportedId: userId,
          reportedName: userName,
          rideId: rideId,
        );
        if (sent && context.mounted) {
          showAppNotice(
            context,
            'Denúncia enviada. Obrigado por avisar.',
            type: AppNoticeType.success,
          );
        }
      case _SafetyAction.block:
        final blocked = await showBlockConfirmDialog(
          context,
          userId: userId,
          userName: userName,
        );
        if (blocked && context.mounted) {
          showAppNotice(
            context,
            '$userName foi bloqueado.',
            type: AppNoticeType.success,
          );
        }
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<_SafetyAction>(
      tooltip: 'Denunciar ou bloquear',
      color: AppColors.cardBackground,
      surfaceTintColor: AppColors.cardBackground,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      icon: const Icon(Icons.more_vert_rounded, color: AppColors.textSecondary),
      onOpened: HapticFeedback.lightImpact,
      onSelected: (action) => _onSelected(context, action),
      itemBuilder: (_) => const [
        PopupMenuItem(
          value: _SafetyAction.report,
          child: _MenuRow(
            icon: Icons.flag_rounded,
            label: 'Denunciar',
            color: AppColors.textPrimary,
          ),
        ),
        PopupMenuItem(
          value: _SafetyAction.block,
          child: _MenuRow(
            icon: Icons.block_rounded,
            label: 'Bloquear',
            color: AppColors.error,
          ),
        ),
      ],
    );
  }
}

class _MenuRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;

  const _MenuRow({
    required this.icon,
    required this.label,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 20, color: color),
        const SizedBox(width: 10),
        Text(
          label,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: color,
          ),
        ),
      ],
    );
  }
}
