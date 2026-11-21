// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/chat/presentation/widgets/chat_message_list.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _list({required bool rideFinished}) => MaterialApp(
  home: Scaffold(
    body: ChatMessageList(
      messages: const [],
      viewerUid: 'uid-1',
      rideFinished: rideFinished,
    ),
  ),
);

void main() {
  testWidgets('conversa vazia de uma viagem que vai acontecer convida a combinar a saída', (
    tester,
  ) async {
    await tester.pumpWidget(_list(rideFinished: false));

    expect(find.text('Conversa liberada'), findsOneWidget);
    expect(
      find.text('Combinem por aqui o ponto de encontro da carona.'),
      findsOneWidget,
    );
  });

  testWidgets('conversa vazia de uma viagem concluída não fala em combinar a saída', (
    tester,
  ) async {
    await tester.pumpWidget(_list(rideFinished: true));

    expect(find.text('Viagem concluída'), findsOneWidget);
    expect(find.text('Conversa liberada'), findsNothing);
    expect(find.textContaining('ponto de encontro'), findsNothing);
  });
}
