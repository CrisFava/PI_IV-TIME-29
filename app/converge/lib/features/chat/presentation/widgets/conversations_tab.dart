// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/state/ride_providers.dart';
import '../../data/models/chat_conversation.dart';
import '../../state/chat_providers.dart';
import 'conversation_tile.dart';
import 'conversations_empty_state.dart';

/// Aba "Mensagens": as conversas das caronas em que o motorista já aceitou o passageiro.
class ConversationsTab extends ConsumerWidget {
  final double bottomPadding;

  const ConversationsTab({super.key, required this.bottomPadding});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final conversations = ref.watch(conversationsProvider);
    final padding = EdgeInsets.fromLTRB(16, 16, 16, 120 + bottomPadding);

    return RefreshIndicator(
      color: AppColors.primary,
      backgroundColor: AppColors.cardBackground,
      onRefresh: () async {
        ref.invalidate(myRidesProvider);
        // O erro, se houver, aparece na própria lista.
        await ref
            .read(conversationsProvider.future)
            .catchError((Object _) => const <ChatConversation>[]);
      },
      child: conversations.when(
        loading: () => ListView(
          padding: padding,
          children: const [
            SizedBox(height: 80),
            Center(
              child: CircularProgressIndicator(
                color: AppColors.primary,
                strokeWidth: 2.8,
              ),
            ),
          ],
        ),
        error: (error, _) => ListView(
          padding: padding,
          children: [
            ConversationsEmptyState(
              icon: Icons.cloud_off_rounded,
              title: 'Não foi possível carregar as conversas',
              message: chatErrorMessage(error),
              actionLabel: 'Tentar novamente',
              onAction: () => ref.invalidate(myRidesProvider),
            ),
          ],
        ),
        data: (conversations) => ListView(
          padding: padding,
          physics: const AlwaysScrollableScrollPhysics(),
          children: [
            const Text(
              'Mensagens das Caronas',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 12),
            if (conversations.isEmpty)
              const ConversationsEmptyState(
                icon: Icons.forum_rounded,
                title: 'Nenhuma conversa ainda',
                message: 'O chat é liberado quando o motorista aceita o pedido de carona.',
              )
            else
              for (final conversation in conversations)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: ConversationTile(conversation: conversation),
                ),
          ],
        ),
      ),
    );
  }
}
