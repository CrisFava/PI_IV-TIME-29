// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/home/presentation/widgets/ride_flow/ride_schedule_step_sheet.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

const _submitKey = ValueKey('ride_flow_submit');
const _hint = 'Escolha o dia e o horário para continuar.';

Future<List<int>> _pumpSheet(
  WidgetTester tester, {
  required bool isDriver,
  required bool hasDeparture,
  bool isSubmitting = false,
}) async {
  tester.view.physicalSize = const Size(800, 1600);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  // Um contador de toques no botão, para conferir se o pedido seguiu
  final submits = <int>[];
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Stack(
          children: [
            Positioned.fill(
              child: RideScheduleStepSheet(
                isDriver: isDriver,
                departureLabel: hasDeparture
                    ? 'Hoje, 07:30'
                    : 'Escolher dia e horário',
                hasDeparture: hasDeparture,
                onChangeDeparture: () {},
                isSubmitting: isSubmitting,
                onSubmit: () => submits.add(1),
                onBack: () {},
                onClose: () {},
              ),
            ),
          ],
        ),
      ),
    ),
  );
  await tester.pump();
  return submits;
}

bool _isSubmitEnabled(WidgetTester tester) =>
    tester.widget<FilledButton>(find.byKey(_submitKey)).onPressed != null;

/// Testes unitários e de widgets para ride schedule step sheet.
/// Cobre cenários como: passageiro.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  group('passageiro', () {
    testWidgets('sem horário, "Revisar pedido" fica desligado e a tela explica o motivo', (
      tester,
    ) async {
      final submits = await _pumpSheet(
        tester,
        isDriver: false,
        hasDeparture: false,
      );

      expect(find.text('Revisar pedido'), findsOneWidget);
      expect(_isSubmitEnabled(tester), isFalse);
      expect(find.text(_hint), findsOneWidget);

      await tester.tap(find.byKey(_submitKey));
      expect(submits, isEmpty);
    });

    testWidgets('com horário escolhido, o botão liga e segue para a revisão', (
      tester,
    ) async {
      final submits = await _pumpSheet(
        tester,
        isDriver: false,
        hasDeparture: true,
      );

      expect(_isSubmitEnabled(tester), isTrue);
      expect(find.text(_hint), findsNothing);

      await tester.tap(find.byKey(_submitKey));
      expect(submits, hasLength(1));
    });
  });

  testWidgets('motorista sem horário também não pode continuar', (tester) async {
    final submits = await _pumpSheet(
      tester,
      isDriver: true,
      hasDeparture: false,
    );

    expect(find.text('Continuar'), findsOneWidget);
    expect(_isSubmitEnabled(tester), isFalse);
    expect(find.text(_hint), findsOneWidget);

    await tester.tap(find.byKey(_submitKey));
    expect(submits, isEmpty);
  });

  testWidgets('enquanto envia, o botão segue desligado e sem a dica', (
    tester,
  ) async {
    await _pumpSheet(
      tester,
      isDriver: false,
      hasDeparture: true,
      isSubmitting: true,
    );

    expect(_isSubmitEnabled(tester), isFalse);
    expect(find.text(_hint), findsNothing);
  });
}
