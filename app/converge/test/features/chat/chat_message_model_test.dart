// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/chat/data/models/chat_message_model.dart';
import 'package:converge/features/chat/data/models/chat_message_type.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('ChatMessageModel.fromMap', () {
    test('lê uma mensagem de texto do Realtime Database', () {
      final message = ChatMessageModel.fromMap('-Nabc', const {
        'senderId': 'driver-1',
        'senderName': 'Lucas Silveira',
        'senderUid': 'firebase-uid',
        'type': 'TEXT',
        'text': 'Chego em 5 minutos',
        'sentAt': 1794816000000,
      });

      expect(message.id, '-Nabc');
      expect(message.senderId, 'driver-1');
      expect(message.senderName, 'Lucas Silveira');
      expect(message.type, ChatMessageType.text);
      expect(message.text, 'Chego em 5 minutos');
      expect(message.amount, isNull);
      expect(message.sentAt, DateTime.fromMillisecondsSinceEpoch(1794816000000));
      expect(message.isFrom('driver-1'), isTrue);
      expect(message.isFrom('passenger-1'), isFalse);
    });

    test('lê o valor das mensagens de pagamento, inteiro ou decimal', () {
      final decimal = ChatMessageModel.fromMap('a', const {
        'type': 'PAYMENT_REQUEST',
        'amount': 12.5,
      });
      final integer = ChatMessageModel.fromMap('b', const {
        'type': 'PAYMENT_CONFIRMED',
        'amount': 12,
      });

      expect(decimal.type, ChatMessageType.paymentRequest);
      expect(decimal.amount, 12.5);
      expect(integer.type, ChatMessageType.paymentConfirmed);
      expect(integer.amount, 12.0);
    });

    test('mensagem malformada vira texto vazio em vez de quebrar a conversa', () {
      final message = ChatMessageModel.fromMap('x', const {
        'type': 'TIPO_NOVO',
        'text': 42,
        'amount': 'doze',
        'sentAt': 'ontem',
      });

      expect(message.type, ChatMessageType.text);
      expect(message.text, '');
      expect(message.senderId, '');
      expect(message.amount, isNull);
      expect(message.sentAt, isNull);
    });
  });

  group('ChatMessageModel.toMap', () {
    test('grava só os campos da mensagem, sem valor quando não é pagamento', () {
      const message = ChatMessageModel(
        senderId: 'driver-1',
        senderName: 'Lucas Silveira',
        type: ChatMessageType.text,
        text: 'Oi',
      );

      expect(message.toMap(), {
        'senderId': 'driver-1',
        'senderName': 'Lucas Silveira',
        'type': 'TEXT',
        'text': 'Oi',
      });
    });

    test('a cobrança leva o valor, e nunca o código PIX', () {
      const message = ChatMessageModel(
        senderId: 'driver-1',
        senderName: 'Lucas Silveira',
        type: ChatMessageType.paymentRequest,
        text: 'Pagamento solicitado.',
        amount: 12.5,
      );

      expect(message.toMap()['amount'], 12.5);
      expect(message.toMap().keys, isNot(contains('pixCopyPaste')));
    });
  });

  test('só mensagens de texto não mudam o estado da viagem', () {
    expect(ChatMessageType.text.changesRideState, isFalse);
    expect(ChatMessageType.paymentRequest.changesRideState, isTrue);
    expect(ChatMessageType.paymentConfirmed.changesRideState, isTrue);
    expect(ChatMessageType.system.changesRideState, isTrue);
  });
}
