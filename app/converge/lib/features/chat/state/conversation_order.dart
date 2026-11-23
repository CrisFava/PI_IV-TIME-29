// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/ride_status.dart';
import '../data/models/chat_conversation.dart';
import '../data/models/chat_thread_ref.dart';
import 'chat_providers.dart';

/// A ordem das conversas na aba "Mensagens": a viagem em andamento fica sempre em
/// cima, e as outras vêm da conversa mais recente para a mais antiga.
///
/// A hora de cada conversa é a da última mensagem ([lastActivity]). Sem mensagem,
/// vale a hora da viagem. Quando duas empatam, fica a ordem que já vinha.
List<ChatConversation> orderConversations(
  List<ChatConversation> conversations, {
  required DateTime? Function(ChatConversation conversation) lastActivity,
}) {
  final entries = [
    for (final (index, conversation) in conversations.indexed)
      (
        index: index,
        conversation: conversation,
        at:
            lastActivity(conversation) ??
            conversation.ride.departureDateTime ??
            DateTime(0),
      ),
  ];

  int rank(ChatConversation conversation) =>
      conversation.ride.status == RideStatus.inProgress ? 0 : 1;

  entries.sort((a, b) {
    final byRank = rank(a.conversation).compareTo(rank(b.conversation));
    if (byRank != 0) return byRank;

    final byTime = b.at.compareTo(a.at);
    return byTime != 0 ? byTime : a.index.compareTo(b.index);
  });

  return [for (final entry in entries) entry.conversation];
}

/// A hora da última mensagem da conversa, ou `null` se ainda não há nenhuma.
final lastMessageAtProvider = Provider.autoDispose
    .family<DateTime?, ChatThreadRef>((ref, thread) {
      final messages = ref.watch(chatMessagesProvider(thread)).value ?? const [];
      DateTime? latest;
      for (final message in messages) {
        final sentAt = message.sentAt;
        if (sentAt != null && (latest == null || sentAt.isAfter(latest))) {
          latest = sentAt;
        }
      }
      return latest;
    });

/// As conversas já na ordem da tela. Reordena sozinha quando chega uma mensagem.
final orderedConversationsProvider =
    Provider.autoDispose<AsyncValue<List<ChatConversation>>>((ref) {
      return ref.watch(conversationsProvider).whenData(
        (conversations) => orderConversations(
          conversations,
          lastActivity: (conversation) =>
              ref.watch(lastMessageAtProvider(conversation.thread)),
        ),
      );
    });
