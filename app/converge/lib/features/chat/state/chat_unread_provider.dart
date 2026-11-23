// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/chat_message_model.dart';
import '../data/models/chat_thread_ref.dart';
import '../data/repository/chat_read_store.dart';
import 'chat_providers.dart';

/// Até quando o usuário logado leu cada conversa (caminho da conversa → data).
final chatReadProvider =
    AsyncNotifierProvider<ChatReadController, Map<String, DateTime>>(
      ChatReadController.new,
    );

class ChatReadController extends AsyncNotifier<Map<String, DateTime>> {
  @override
  Future<Map<String, DateTime>> build() async {
    final viewer = ref.watch(chatViewerProvider);
    if (viewer == null) return const {};
    return ref.read(chatReadStoreProvider).load(viewer.id);
  }

  /// A conversa está aberta na tela: tudo o que chegou até agora foi visto.
  Future<void> markRead(String threadPath) async {
    final viewer = ref.read(chatViewerProvider);
    if (viewer == null) return;
    final now = DateTime.now();
    state = AsyncData({...?state.value, threadPath: now});
    await ref.read(chatReadStoreProvider).save(viewer.id, threadPath, now);
  }
}

/// Quantas mensagens dos outros o usuário ainda não viu: as que chegaram depois da
/// última vez que ele abriu a conversa ([lastRead]), ou todas, se nunca abriu.
int unreadMessages(
  List<ChatMessageModel> messages, {
  required DateTime? lastRead,
  required String viewerUid,
}) {
  var unread = 0;
  for (final message in messages) {
    final sentAt = message.sentAt;
    final isNew =
        lastRead == null || (sentAt != null && sentAt.isAfter(lastRead));
    if (isNew && !message.isSentBy(viewerUid)) unread++;
  }
  return unread;
}

/// Mensagens dos outros que o usuário ainda não viu em uma conversa. É o número
/// na linha da conversa, na aba "Mensagens".
final conversationUnreadProvider = Provider.autoDispose
    .family<int, ChatThreadRef>((ref, thread) {
      final viewer = ref.watch(chatViewerProvider);
      final reads = ref.watch(chatReadProvider).value;
      if (viewer == null || reads == null) return 0;

      final messages = ref.watch(chatMessagesProvider(thread)).value ?? const [];
      return unreadMessages(
        messages,
        lastRead: reads[thread.path],
        viewerUid: viewer.firebaseUid,
      );
    });

/// Mensagens dos outros participantes que o usuário ainda não viu, somando todas
/// as conversas. É o número na aba "Chat" da navbar.
final chatUnreadCountProvider = Provider.autoDispose<int>((ref) {
  final conversations = ref.watch(conversationsProvider).value ?? const [];
  return conversations.fold(
    0,
    (total, conversation) =>
        total + ref.watch(conversationUnreadProvider(conversation.thread)),
  );
});
