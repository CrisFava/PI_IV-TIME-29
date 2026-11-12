// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../../../core/utils/currency_formatter.dart';
import '../../data/models/chat_message_model.dart';
import '../../data/models/chat_message_type.dart';
import 'chat_event_notice.dart';
import 'chat_text_bubble.dart';
import 'chat_time.dart';

/// Uma mensagem da conversa: balão para texto, aviso centralizado para
/// eventos de pagamento e da viagem.
class ChatMessageBubble extends StatelessWidget {
  final ChatMessageModel message;
  final bool isMine;

  const ChatMessageBubble({
    super.key,
    required this.message,
    required this.isMine,
  });

  @override
  Widget build(BuildContext context) => switch (message.type) {
    ChatMessageType.text => ChatTextBubble(message: message, isMine: isMine),
    ChatMessageType.paymentRequest => ChatEventNotice(
      icon: Icons.pix_rounded,
      text: _withAmount('Pagamento solicitado'),
      time: formatChatTime(message.sentAt),
    ),
    ChatMessageType.paymentConfirmed => ChatEventNotice(
      icon: Icons.check_circle_rounded,
      text: _withAmount('Pagamento confirmado'),
      time: formatChatTime(message.sentAt),
      success: true,
    ),
    ChatMessageType.system => ChatEventNotice(
      icon: Icons.info_rounded,
      text: message.text,
      time: formatChatTime(message.sentAt),
    ),
  };

  String _withAmount(String label) {
    final amount = message.amount;
    return amount == null
        ? label
        : '$label: ${CurrencyFormatter.format(amount)}';
  }
}
