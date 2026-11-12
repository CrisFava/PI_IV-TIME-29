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

  /// Id do usuário no Converge (o mesmo da API). Gravado pelo app, serve só para exibição.
  final String senderId;

  /// Uid do Firebase de quem enviou. As regras do banco garantem que é o de quem está
  /// logado, então é este campo que decide se a mensagem é minha.
  final String senderUid;
  final String senderName;
  final ChatMessageType type;
  final String text;

  /// Valor da cobrança, nas mensagens de pagamento.
  final double? amount;
  final DateTime? sentAt;

  const ChatMessageModel({
    this.id = '',
    required this.senderId,
    this.senderUid = '',
    required this.senderName,
    required this.type,
    required this.text,
    this.amount,
    this.sentAt,
  });

  bool isSentBy(String firebaseUid) =>
      senderUid.isNotEmpty && senderUid == firebaseUid;

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
      senderUid: text('senderUid'),
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
  /// (`senderUid`) são acrescentados pelo repositório.
  Map<String, Object?> toMap() => {
    'senderId': senderId,
    'senderName': senderName,
    'type': type.value,
    'text': text,
    'amount': ?amount,
  };
}
