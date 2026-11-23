// Autor: Sara Fernandes Monteiro - RA 25024107

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

/// Testes unitários e de widgets para conversation list info.
/// Cobre cenários como: hora da última mensagem na lista, mensagens não lidas de uma conversa.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
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
