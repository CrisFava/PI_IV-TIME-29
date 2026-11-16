// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/home/presentation/widgets/departure_time_sheet.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('atalhos só aparecem hoje; "Agora" devolve a saída imediata', (
    tester,
  ) async {
    final chosen = <DateTime?>[];
    Widget picker(DateTime day) => MaterialApp(
      home: Scaffold(
        body: DepartureTimePicker(
          key: ValueKey(day),
          day: day,
          onChosen: chosen.add,
        ),
      ),
    );

    await tester.pumpWidget(picker(DateTime.now()));
    await tester.tap(find.text('Agora'));
    expect(chosen, [null]);

    final tomorrow = DateTime.now().add(const Duration(days: 1));
    await tester.pumpWidget(picker(tomorrow));
    expect(find.text('Agora'), findsNothing);
    await tester.tap(find.byKey(const ValueKey('confirm_departure_time')));
    expect(chosen.last?.day, tomorrow.day);
  });
}
