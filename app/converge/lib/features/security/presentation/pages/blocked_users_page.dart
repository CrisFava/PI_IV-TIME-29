// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../data/models/block_model.dart';
import '../../state/blocked_users_controller.dart';

/// Lista de usuários bloqueados, com o botão de desbloquear (RF46).
class BlockedUsersPage extends ConsumerStatefulWidget {
  const BlockedUsersPage({super.key});

  @override
  ConsumerState<BlockedUsersPage> createState() => _BlockedUsersPageState();
}

class _BlockedUsersPageState extends ConsumerState<BlockedUsersPage> {
  /// Ids que estão sendo desbloqueados agora.
  final Set<String> _busy = {};

  // DELETE /blocks/{id} pelo controller. Só a linha tocada fica carregando; as outras continuam livres.
  Future<void> _unblock(BlockModel block) async {
    if (_busy.contains(block.blockedUserId)) return;
    HapticFeedback.mediumImpact();
    setState(() => _busy.add(block.blockedUserId));
    try {
      await ref
          .read(blockedUsersProvider.notifier)
          .unblock(block.blockedUserId);
      if (!mounted) return;
      showAppNotice(
        context,
        '${block.displayName} foi desbloqueado.',
        type: AppNoticeType.success,
      );
    } catch (error) {
      if (!mounted) return;
      showAppNotice(
        context,
        authErrorMessage(error),
        type: AppNoticeType.error,
      );
    } finally {
      if (mounted) setState(() => _busy.remove(block.blockedUserId));
    }
  }

  @override
  Widget build(BuildContext context) {
    // A lista vem de GET /blocks. O provider já tira da lista quem foi desbloqueado.
    final blocks = ref.watch(blockedUsersProvider);
    return Scaffold(
      backgroundColor: AppColors.scaffoldBackground,
      appBar: AppBar(
        backgroundColor: AppColors.cardBackground,
        surfaceTintColor: AppColors.cardBackground,
        foregroundColor: AppColors.textPrimary,
        title: const Text(
          'Bloqueados',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
        ),
      ),
      body: blocks.when(
        loading: () => const Center(
          child: CircularProgressIndicator(
            color: AppColors.primary,
            strokeWidth: 2.8,
          ),
        ),
        error: (error, _) => _Message(
          icon: Icons.error_outline_rounded,
          title: 'Não foi possível carregar',
          message: authErrorMessage(error),
          actionLabel: 'Tentar novamente',
          onAction: () => ref.invalidate(blockedUsersProvider),
        ),
        data: (list) {
          if (list.isEmpty) {
            return const _Message(
              icon: Icons.block_rounded,
              title: 'Nenhum usuário bloqueado',
              message: 'Quando você bloquear alguém, a pessoa aparece aqui para você desbloquear.',
            );
          }
          // Puxar para baixo busca a lista de novo no backend.
          return RefreshIndicator(
            color: AppColors.primary,
            onRefresh: () async {
              ref.invalidate(blockedUsersProvider);
              await ref.read(blockedUsersProvider.future);
            },
            child: ListView.separated(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: EdgeInsets.fromLTRB(
                16,
                16,
                16,
                24 + MediaQuery.paddingOf(context).bottom,
              ),
              itemCount: list.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (_, index) => _BlockedTile(
                block: list[index],
                busy: _busy.contains(list[index].blockedUserId),
                onUnblock: () => _unblock(list[index]),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// Uma linha da lista: nome da pessoa bloqueada e o botão "Desbloquear".
class _BlockedTile extends StatelessWidget {
  final BlockModel block;
  final bool busy;
  final VoidCallback onUnblock;

  const _BlockedTile({
    required this.block,
    required this.busy,
    required this.onUnblock,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 20,
            backgroundColor: AppColors.neutralLight,
            child: Icon(Icons.person_rounded, color: AppColors.primary),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              block.displayName,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
          ),
          const SizedBox(width: 8),
          OutlinedButton(
            onPressed: busy ? null : onUnblock,
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.textPrimary,
              side: const BorderSide(color: AppColors.borderMedium),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            child: busy
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.primary,
                    ),
                  )
                : const Text(
                    'Desbloquear',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
          ),
        ],
      ),
    );
  }
}

/// Tela de aviso usada para a lista vazia e para o erro ao carregar (com "Tentar novamente").
class _Message extends StatelessWidget {
  final IconData icon;
  final String title;
  final String message;
  final String? actionLabel;
  final VoidCallback? onAction;

  const _Message({
    required this.icon,
    required this.title,
    required this.message,
    this.actionLabel,
    this.onAction,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 40, color: AppColors.iconMuted),
            const SizedBox(height: 12),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 13.5,
                color: AppColors.textSecondary,
              ),
            ),
            if (actionLabel != null) ...[
              const SizedBox(height: 14),
              OutlinedButton(onPressed: onAction, child: Text(actionLabel!)),
            ],
          ],
        ),
      ),
    );
  }
}
