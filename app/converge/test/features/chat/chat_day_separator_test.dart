// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/chat/data/models/chat_message_model.dart';
import 'package:converge/features/chat/data/models/chat_message_type.dart';
import 'package:converge/features/chat/presentation/widgets/chat_message_list.dart';
import 'package:converge/features/chat/presentation/widgets/chat_time.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

ChatMessageModel _message(String id, DateTime sentAt) => ChatMessageModel(
  id: id,
  senderId: 'outro',
  senderUid: 'uid-outro',
  senderName: 'Outro',
  type: ChatMessageType.text,
  text: 'mensagem $id',
  sentAt: sentAt,
);

Future<void> _pump(WidgetTester tester, List<ChatMessageModel> messages) async {
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: ChatMessageList(messages: messages, viewerUid: 'uid-eu'),
      ),
    ),
  );
}

/// Testes unitários e de widgets para chat day separator.
/// Cobre cenários como: uma etiqueta para cada dia, só quando o dia muda, conversa de um dia só tem uma etiqueta.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  testWidgets('uma etiqueta para cada dia, só quando o dia muda', (
    tester,
  ) async {
    final now = DateTime.now();
    final threeDaysAgo = now.subtract(const Duration(days: 3));

    await _pump(tester, [
      _message('a', threeDaysAgo),
      _message('b', now.subtract(const Duration(days: 1))),
      // Duas mensagens no mesmo dia: uma etiqueta só
      _message('c', now),
      _message('d', now),
    ]);

    expect(find.text(formatChatDay(threeDaysAgo, now: now)), findsOneWidget);
    expect(find.text('Ontem'), findsOneWidget);
    expect(find.text('Hoje'), findsOneWidget);
  });

  testWidgets('conversa de um dia só tem uma etiqueta', (tester) async {
    final now = DateTime.now();

    await _pump(tester, [_message('a', now), _message('b', now)]);

    expect(find.text('Hoje'), findsOneWidget);
    expect(find.text('Ontem'), findsNothing);
  });
}
