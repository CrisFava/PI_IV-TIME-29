// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/core/widgets/sheet_drag_to_dismiss.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Future<void> pumpSheet(WidgetTester tester, VoidCallback onDismiss) {
    return tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SheetDragToDismiss(
            onDismiss: onDismiss,
            child: DraggableScrollableSheet(
              initialChildSize: 0.5,
              minChildSize: 0.1,
              maxChildSize: 0.9,
              snap: true,
              snapSizes: const [0.22, 0.5, 0.9],
              builder: (context, controller) => ListView(
                controller: controller,
                children: [
                  for (var i = 0; i < 30; i++)
                    SizedBox(height: 40, child: Text('item $i')),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  testWidgets('arrastar um pouco para baixo não fecha', (tester) async {
    var dismissed = 0;
    await pumpSheet(tester, () => dismissed++);

    // 600 px de tela: de 50% para cerca de 35%
    await tester.drag(find.text('item 0'), const Offset(0, 90));
    await tester.pumpAndSettle();

    expect(dismissed, 0);
  });

  testWidgets('arrastar abaixo do menor encaixe fecha uma vez', (tester) async {
    var dismissed = 0;
    await pumpSheet(tester, () => dismissed++);

    // De 50% para perto de 10%, abaixo do encaixe de 22%
    await tester.drag(find.text('item 0'), const Offset(0, 240));
    await tester.pumpAndSettle();

    expect(dismissed, 1);
  });
}
