// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../data/models/chat_message_model.dart';
import 'chat_message_bubble.dart';
import 'chat_notice.dart';

/// Lista de mensagens da conversa, da mais antiga para a mais nova.
class ChatMessageList extends StatelessWidget {
  final List<ChatMessageModel> messages;
  final String viewerId;

  const ChatMessageList({
    super.key,
    required this.messages,
    required this.viewerId,
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

    // Invertida: a lista nasce no fim e acompanha as mensagens novas.
    return ListView.builder(
      reverse: true,
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
      itemCount: messages.length,
      itemBuilder: (context, index) {
        final message = messages[messages.length - 1 - index];
        return ChatMessageBubble(
          key: ValueKey(message.id),
          message: message,
          isMine: message.isFrom(viewerId),
        );
      },
    );
  }
}
