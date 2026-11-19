// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

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

/// Mensagens dos outros participantes que o usuário ainda não viu, somando todas
/// as conversas. É o número na aba "Chat" da navbar.
final chatUnreadCountProvider = Provider.autoDispose<int>((ref) {
  final viewer = ref.watch(chatViewerProvider);
  final reads = ref.watch(chatReadProvider).value;
  if (viewer == null || reads == null) return 0;

  final conversations = ref.watch(conversationsProvider).value ?? const [];
  var unread = 0;
  for (final conversation in conversations) {
    final messages =
        ref.watch(chatMessagesProvider(conversation.thread)).value ?? const [];
    final lastRead = reads[conversation.thread.path];
    for (final message in messages) {
      final sentAt = message.sentAt;
      final isNew =
          lastRead == null || (sentAt != null && sentAt.isAfter(lastRead));
      if (isNew && !message.isSentBy(viewer.firebaseUid)) unread++;
    }
  }
  return unread;
});
