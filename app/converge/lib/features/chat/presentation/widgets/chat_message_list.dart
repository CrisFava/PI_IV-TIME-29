// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../data/models/chat_message_model.dart';
import 'chat_day_separator.dart';
import 'chat_message_bubble.dart';
import 'chat_notice.dart';
import 'chat_time.dart';

/// Lista de mensagens da conversa, da mais antiga para a mais nova.
class ChatMessageList extends StatelessWidget {
  final List<ChatMessageModel> messages;

  /// Uid do Firebase de quem está logado.
  final String viewerUid;

  const ChatMessageList({
    super.key,
    required this.messages,
    required this.viewerUid,
  });

  @override
  Widget build(BuildContext context) {
    if (messages.isEmpty) {
      return const ChatNotice(
        icon: Icons.forum_rounded,
        title: 'Conversa liberada',
        message: 'Combinem por aqui o ponto de encontro e o valor da carona.',
      );
    }

    final now = DateTime.now();
    // O dia da mensagem; uma que ainda não tem hora está sendo enviada agora
    DateTime dayOf(ChatMessageModel message) {
      final sentAt = (message.sentAt ?? now).toLocal();
      return DateTime(sentAt.year, sentAt.month, sentAt.day);
    }

    // Invertida: a lista nasce no fim e acompanha as mensagens novas.
    return ListView.builder(
      reverse: true,
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
      itemCount: messages.length,
      itemBuilder: (context, index) {
        final position = messages.length - 1 - index;
        final message = messages[position];
        final bubble = ChatMessageBubble(
          key: ValueKey(message.id),
          message: message,
          isMine: message.isSentBy(viewerUid),
        );

        // A etiqueta do dia vai em cima da primeira mensagem de cada dia
        final startsDay =
            position == 0 || dayOf(messages[position - 1]) != dayOf(message);
        if (!startsDay) return bubble;

        return Column(
          key: ValueKey('day_${message.id}'),
          mainAxisSize: MainAxisSize.min,
          children: [
            ChatDaySeparator(label: formatChatDay(dayOf(message), now: now)),
            bubble,
          ],
        );
      },
    );
  }
}
