// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/chat_message_model.dart';
import 'chat_time.dart';

/// Balão de uma mensagem de texto: à direita para quem enviou, à esquerda para o outro.
class ChatTextBubble extends StatelessWidget {
  final ChatMessageModel message;
  final bool isMine;

  const ChatTextBubble({
    super.key,
    required this.message,
    required this.isMine,
  });

  @override
  Widget build(BuildContext context) {
    final foreground = isMine ? AppColors.textLight : AppColors.textPrimary;
    final time = formatChatTime(message.sentAt);
    final sender = isMine
        ? 'Você'
        : (message.senderName.trim().isEmpty
              ? 'Participante'
              : message.senderName);

    // O leitor de tela anuncia quem enviou e o texto uma vez só.
    return Semantics(
      label: time.isEmpty
          ? '$sender: ${message.text}'
          : '$sender: ${message.text}, $time',
      excludeSemantics: true,
      child: Align(
        alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxWidth: MediaQuery.sizeOf(context).width * 0.78,
          ),
          child: Container(
            margin: const EdgeInsets.symmetric(vertical: 3),
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 8),
            decoration: BoxDecoration(
              color: isMine
                  ? AppColors.chatBubbleMine
                  : AppColors.chatBubbleOther,
              borderRadius: BorderRadius.only(
                topLeft: const Radius.circular(18),
                topRight: const Radius.circular(18),
                bottomLeft: Radius.circular(isMine ? 18 : 4),
                bottomRight: Radius.circular(isMine ? 4 : 18),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  message.text,
                  style: TextStyle(
                    fontSize: 14.5,
                    height: 1.3,
                    color: foreground,
                  ),
                ),
                if (time.isNotEmpty) ...[
                  const SizedBox(height: 3),
                  Text(
                    time,
                    style: TextStyle(
                      fontSize: 10.5,
                      color: isMine
                          ? AppColors.dockItemInactive
                          : AppColors.textMuted,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
