// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/core/widgets/show_app_notice.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Future<BuildContext> pumpApp(WidgetTester tester) async {
    late BuildContext captured;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) {
              captured = context;
              return const SizedBox.expand();
            },
          ),
        ),
      ),
    );
    return captured;
  }

  testWidgets('aparece no topo e fecha pelo botão', (tester) async {
    final context = await pumpApp(tester);

    showAppNotice(context, 'Carro cadastrado.');
    await tester.pumpAndSettle();

    final notice = find.text('Carro cadastrado.');
    expect(notice, findsOneWidget);
    // Fica na metade de cima da tela, longe da barra de navegação
    expect(tester.getCenter(notice).dy, lessThan(200));

    await tester.tap(find.byKey(const ValueKey('close_app_notice')));
    await tester.pumpAndSettle();
    expect(notice, findsNothing);
  });

  testWidgets('some sozinho e um aviso novo substitui o anterior', (
    tester,
  ) async {
    final context = await pumpApp(tester);

    showAppNotice(context, 'Primeiro aviso');
    await tester.pumpAndSettle();
    showAppNotice(context, 'Segundo aviso', type: AppNoticeType.error);
    await tester.pumpAndSettle();

    expect(find.text('Primeiro aviso'), findsNothing);
    expect(find.text('Segundo aviso'), findsOneWidget);

    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();
    expect(find.text('Segundo aviso'), findsNothing);
  });
}
