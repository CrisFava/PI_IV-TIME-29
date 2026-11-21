// Autor: Sara Fernandes Monteiro - RA 25024107

import 'dart:io';

import 'package:converge/features/chat/data/models/chat_message_model.dart';
import 'package:converge/features/chat/data/models/chat_message_type.dart';
import 'package:converge/features/chat/presentation/widgets/chat_time.dart';
import 'package:converge/features/chat/state/chat_unread_provider.dart';
import 'package:flutter_test/flutter_test.dart';

ChatMessageModel _message({required bool mine, DateTime? sentAt}) =>
    ChatMessageModel(
      senderId: mine ? 'eu' : 'outro',
      senderUid: mine ? 'uid-eu' : 'uid-outro',
      senderName: mine ? 'Eu' : 'Outro',
      type: ChatMessageType.text,
      text: 'oi',
      sentAt: sentAt,
    );

void main() {
  group('hora da última mensagem na lista', () {
    final now = DateTime(2026, 11, 21, 15);

    test('hoje mostra a hora', () {
      expect(
        formatConversationTime(DateTime(2026, 11, 21, 7, 42), now: now),
        '07:42',
      );
    });

    test('ontem mostra "Ontem"', () {
      expect(
        formatConversationTime(DateTime(2026, 11, 20, 23, 59), now: now),
        'Ontem',
      );
    });

    test(
      'ontem continua sendo Ontem após o início do horário de verão',
      () {
        final now = DateTime(2026, 3, 9);
        final yesterday = DateTime(2026, 3, 8);
        final sentAt = DateTime(2026, 3, 8, 23, 59).toUtc();

        expect(now.difference(yesterday), const Duration(hours: 23));
        expect(
          [
            formatChatDay(sentAt, now: now),
            formatConversationTime(sentAt, now: now),
          ],
          ['Ontem', 'Ontem'],
        );
      },
      skip: Platform.environment['TZ'] != 'America/New_York'
          ? 'Execute com TZ=America/New_York.'
          : null,
    );

    test('antes disso mostra o dia e o mês', () {
      expect(
        formatConversationTime(DateTime(2026, 11, 17, 10), now: now),
        '17/11',
      );
    });
  });

  group('mensagens não lidas de uma conversa', () {
    final read = DateTime(2026, 11, 20, 12);

    test('sem nunca abrir, contam todas as dos outros e nenhuma minha', () {
      final messages = [
        _message(mine: false, sentAt: DateTime(2026, 11, 20, 9)),
        _message(mine: true, sentAt: DateTime(2026, 11, 20, 10)),
        _message(mine: false, sentAt: DateTime(2026, 11, 20, 11)),
      ];

      expect(unreadMessages(messages, lastRead: null, viewerUid: 'uid-eu'), 2);
    });

    test('depois de abrir, contam só as que chegaram depois', () {
      final messages = [
        _message(mine: false, sentAt: DateTime(2026, 11, 20, 9)),
        _message(mine: false, sentAt: DateTime(2026, 11, 20, 13)),
        _message(mine: false, sentAt: DateTime(2026, 11, 20, 14)),
      ];

      expect(unreadMessages(messages, lastRead: read, viewerUid: 'uid-eu'), 2);
    });
  });
}
