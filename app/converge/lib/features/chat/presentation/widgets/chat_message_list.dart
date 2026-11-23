// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../data/models/chat_message_model.dart';
import 'chat_day_separator.dart';
import 'chat_message_bubble.dart';
import 'chat_notice.dart';
import 'chat_time.dart';

/// Lista de mensagens da conversa, da mais antiga para a mais nova.
///
/// Com a lista vazia, mostra um convite para combinar a carona. Separa os dias com uma etiqueta
/// (`Hoje`, `Ontem` ou a data).
class ChatMessageList extends StatelessWidget {
  /// Mensagens em ordem cronológica, como vêm do repositório.
  final List<ChatMessageModel> messages;

  /// Uid do Firebase de quem está logado.
  final String viewerUid;

  /// A viagem já terminou: o texto da conversa vazia deixa de falar em combinar a saída.
  final bool rideFinished;

  const ChatMessageList({
    super.key,
    required this.messages,
    required this.viewerUid,
    this.rideFinished = false,
  });

  @override
  Widget build(BuildContext context) {
    if (messages.isEmpty) {
      return rideFinished
          ? const ChatNotice(
              icon: Icons.flag_rounded,
              title: 'Viagem concluída',
              message: 'A conversa continua aberta para qualquer combinação sobre a carona.',
            )
          : const ChatNotice(
              icon: Icons.forum_rounded,
              title: 'Conversa liberada',
              message: 'Combinem por aqui o ponto de encontro da carona.',
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
