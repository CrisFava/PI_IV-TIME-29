// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/home/presentation/widgets/departure_time_sheet.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Future<void> _pump(WidgetTester tester, {DateTime? current}) => tester.pumpWidget(
  MaterialApp(
    home: Scaffold(
      body: DepartureTimePicker(
        day: DateTime(2026, 11, 21),
        current: current,
        onChosen: (_) {},
      ),
    ),
  ),
);

/// Testes unitários e de widgets para departure time picker.
/// Cobre cenários como: arredondar para a roda de 5 em 5, roda de horário.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  group('arredondar para a roda de 5 em 5', () {
    test('sobe para o próximo múltiplo de 5 e tira os segundos', () {
      expect(
        roundUpToMinuteStep(DateTime(2026, 11, 21, 14, 3, 40)),
        DateTime(2026, 11, 21, 14, 5),
      );
    });

    test('quem já está num múltiplo de 5 não muda', () {
      expect(
        roundUpToMinuteStep(DateTime(2026, 11, 21, 14, 5)),
        DateTime(2026, 11, 21, 14, 5),
      );
    });

    test('passa para a hora seguinte quando preciso', () {
      expect(
        roundUpToMinuteStep(DateTime(2026, 11, 21, 14, 56)),
        DateTime(2026, 11, 21, 15),
      );
    });
  });

  group('roda de horário', () {
    testWidgets('anda de 5 em 5 minutos', (tester) async {
      await _pump(tester, current: DateTime(2026, 11, 21, 14, 5));

      final wheel = tester.widget<CupertinoDatePicker>(
        find.byKey(const ValueKey('departure_time_wheel')),
      );
      expect(wheel.minuteInterval, 5);
    });

    testWidgets('um horário fora da roda abre no próximo da roda', (
      tester,
    ) async {
      // 14:03 não existe na roda: sem o arredondamento ela quebraria ao abrir
      await _pump(tester, current: DateTime(2026, 11, 21, 14, 3));

      expect(find.text('Confirmar 14:05'), findsOneWidget);
    });

    testWidgets('perto da meia-noite, fica no último horário do dia', (
      tester,
    ) async {
      await _pump(tester, current: DateTime(2026, 11, 21, 23, 58));

      expect(find.text('Confirmar 23:55'), findsOneWidget);
    });
  });
}
