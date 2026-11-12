// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import 'chat_message_type.dart';

/// Uma mensagem do chat, como fica guardada no Firebase Realtime Database.
///
/// O código PIX nunca é gravado aqui: a mensagem de cobrança leva só o valor,
/// e o app busca o Copia e Cola no backend, que confere quem está pedindo.
@immutable
class ChatMessageModel {
  /// Chave gerada pelo Realtime Database. Vazia enquanto a mensagem não foi enviada.
  final String id;

  /// Id do usuário no Converge (o mesmo da API).
  final String senderId;
  final String senderName;
  final ChatMessageType type;
  final String text;

  /// Valor da cobrança, nas mensagens de pagamento.
  final double? amount;
  final DateTime? sentAt;

  const ChatMessageModel({
    this.id = '',
    required this.senderId,
    required this.senderName,
    required this.type,
    required this.text,
    this.amount,
    this.sentAt,
  });

  bool isFrom(String userId) => senderId == userId;

  /// Lê o valor de um nó `messages/{id}`. Campos ausentes ou de tipo errado
  /// viram valores neutros, para uma mensagem malformada não derrubar a conversa.
  factory ChatMessageModel.fromMap(String id, Map<Object?, Object?> map) {
    final sentAt = map['sentAt'];
    final amount = map['amount'];
    String text(String key) => switch (map[key]) {
      final String value => value,
      _ => '',
    };

    return ChatMessageModel(
      id: id,
      senderId: text('senderId'),
      senderName: text('senderName'),
      type: ChatMessageType.fromValue(map['type']),
      text: text('text'),
      amount: amount is num ? amount.toDouble() : null,
      sentAt: sentAt is num
          ? DateTime.fromMillisecondsSinceEpoch(sentAt.toInt())
          : null,
    );
  }

  /// Campos gravados no Realtime Database. O horário e o uid de quem envia
  /// são acrescentados pelo repositório.
  Map<String, Object?> toMap() => {
    'senderId': senderId,
    'senderName': senderName,
    'type': type.value,
    'text': text,
    'amount': ?amount,
  };
}
