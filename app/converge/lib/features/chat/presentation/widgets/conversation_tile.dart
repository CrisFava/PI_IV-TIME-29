// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app_routes.dart';
import '../../../../core/config/styles.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../data/models/chat_conversation.dart';
import '../../state/chat_unread_provider.dart';
import '../../state/conversation_order.dart';
import 'chat_time.dart';
import 'live_dot.dart';

/// Uma conversa na aba "Mensagens". Ao tocar, abre o chat. Mostra a hora da última
/// mensagem e quantas ainda não foram vistas.
class ConversationTile extends ConsumerWidget {
  final ChatConversation conversation;

  const ConversationTile({super.key, required this.conversation});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ride = conversation.ride;
    final lastMessageAt = ref.watch(lastMessageAtProvider(conversation.thread));
    final unread = ref.watch(conversationUnreadProvider(conversation.thread));
    final when = _date(ride.departureDateTime);
    final inProgress = ride.status == RideStatus.inProgress;
    final highlighted = ride.status == RideStatus.confirmed || inProgress;

    return Material(
      color: AppColors.cardBackground,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () {
          HapticFeedback.lightImpact();
          context.push(
            AppRoutes.chatOf(
              rideId: conversation.thread.rideId,
              passengerId: conversation.thread.passengerId,
            ),
          );
        },
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            // A viagem em andamento se destaca com a borda verde
            border: Border.all(
              color: inProgress ? AppColors.success : AppColors.borderSubtle,
              width: inProgress ? 1.5 : 1,
            ),
          ),
          child: Row(
            children: [
              const CircleAvatar(
                radius: 20,
                backgroundColor: AppColors.neutralLight,
                child: Icon(
                  Icons.forum_rounded,
                  color: AppColors.textPrimary,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            // Grupo da viagem: o motorista vê os passageiros; o passageiro, o motorista
                            conversation.viewerIsDriver
                                ? 'Grupo: ${conversation.otherPartyName}'
                                : 'Carona com ${conversation.otherPartyName}',
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontWeight: FontWeight.w700,
                              fontSize: 14,
                              color: AppColors.textPrimary,
                            ),
                          ),
                        ),
                        if (lastMessageAt != null) ...[
                          const SizedBox(width: 8),
                          // A hora da última mensagem, que é o que ordena a lista
                          Text(
                            formatConversationTime(
                              lastMessageAt,
                              now: DateTime.now(),
                            ),
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: unread > 0
                                  ? FontWeight.w700
                                  : FontWeight.w400,
                              color: unread > 0
                                  ? AppColors.textPrimary
                                  : AppColors.textMuted,
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${ride.origin.address} → ${ride.destination.address}',
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        // Só a viagem que está acontecendo agora
                        if (inProgress) ...[
                          const LiveDot(),
                          const SizedBox(width: 6),
                        ],
                        Expanded(
                          // A viagem: situação e quando sai (ou saiu)
                          child: Text(
                            when.isEmpty
                                ? ride.status.label
                                : '${ride.status.label} · $when',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 11.5,
                              fontWeight: FontWeight.w700,
                              color: highlighted
                                  ? AppColors.success
                                  : AppColors.textMuted,
                            ),
                          ),
                        ),
                        if (unread > 0) _UnreadBadge(count: unread),
                      ],
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.chevron_right_rounded,
                color: AppColors.iconMuted,
              ),
            ],
          ),
        ),
      ),
    );
  }

  static String _date(DateTime? date) {
    if (date == null) return '';
    return '${date.day.toString().padLeft(2, '0')}/'
        '${date.month.toString().padLeft(2, '0')} '
        '${date.hour.toString().padLeft(2, '0')}:'
        '${date.minute.toString().padLeft(2, '0')}';
  }
}

/// Quantas mensagens da conversa ainda não foram vistas.
class _UnreadBadge extends StatelessWidget {
  final int count;

  const _UnreadBadge({required this.count});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '$count não lidas',
      child: Container(
        key: const ValueKey('conversation_unread_badge'),
        constraints: const BoxConstraints(minWidth: 20),
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(
          color: AppColors.primary,
          borderRadius: BorderRadius.circular(10),
        ),
        child: ExcludeSemantics(
          child: Text(
            count > 99 ? '99+' : '$count',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w800,
              color: AppColors.textLight,
            ),
          ),
        ),
      ),
    );
  }
}
