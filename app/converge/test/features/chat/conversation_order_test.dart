// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/chat/data/models/chat_conversation.dart';
import 'package:converge/features/chat/state/conversation_order.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';

/// Uma conversa de cada sala, vista pela passageira. A sala identifica a conversa.
ChatConversation _conversation(String room, {String status = 'CONFIRMED'}) =>
    ChatConversation.ofRide(
      fakeRide(status: status, chatRoomId: room),
      fakePassengerId,
    ).single;

List<String> _order(
  List<ChatConversation> conversations,
  Map<String, DateTime> lastMessage,
) => [
  for (final conversation in orderConversations(
    conversations,
    lastActivity: (conversation) => lastMessage[conversation.thread.chatRoomId],
  ))
    conversation.thread.chatRoomId,
];

/// Testes unitários e de widgets para conversation order.
/// Cobre cenários como: a viagem em andamento fica em cima, mesmo com mensagem antiga, o resto vem da última mensagem mais recente para a mais antiga.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  test('a viagem em andamento fica em cima, mesmo com mensagem antiga', () {
    final order = _order(
      [
        _conversation('antiga', status: 'COMPLETED'),
        _conversation('andamento', status: 'IN_PROGRESS'),
        _conversation('nova'),
      ],
      {
        'andamento': DateTime(2026, 11, 1),
        'antiga': DateTime(2026, 11, 20, 9),
        'nova': DateTime(2026, 11, 20, 10),
      },
    );

    expect(order.first, 'andamento');
  });

  test('o resto vem da última mensagem mais recente para a mais antiga', () {
    // Confirmada e concluída misturadas: só a mensagem decide
    final order = _order(
      [
        _conversation('confirmada-antiga'),
        _conversation('concluida-nova', status: 'COMPLETED'),
        _conversation('confirmada-meio'),
      ],
      {
        'confirmada-antiga': DateTime(2026, 11, 18),
        'concluida-nova': DateTime(2026, 11, 20),
        'confirmada-meio': DateTime(2026, 11, 19),
      },
    );

    expect(order, ['concluida-nova', 'confirmada-meio', 'confirmada-antiga']);
  });

  test('sem mensagem, a conversa vale pela hora da viagem', () {
    // A viagem do fake é em 16/11: uma mensagem de 10/11 fica abaixo dela
    final order = _order(
      [_conversation('so-mensagem-velha'), _conversation('sem-mensagem')],
      {'so-mensagem-velha': DateTime(2026, 11, 10)},
    );

    expect(order, ['sem-mensagem', 'so-mensagem-velha']);
  });
}
