// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/home/presentation/widgets/passenger_ride_step.dart';
import 'package:converge/features/home/presentation/widgets/ride_flow/discard_ride_dialog.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Abre o aviso e devolve uma função com o que a pessoa respondeu.
Future<bool? Function()> _openDialog(
  WidgetTester tester, {
  required bool isDriver,
}) async {
  bool? answer;
  await tester.pumpWidget(
    MaterialApp(
      home: Builder(
        builder: (context) => Scaffold(
          body: Center(
            child: TextButton(
              onPressed: () async {
                answer = await showDiscardRideDialog(
                  context,
                  isDriver: isDriver,
                );
              },
              child: const Text('abrir'),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('abrir'));
  await tester.pumpAndSettle();
  return () => answer;
}

/// Testes unitários e de widgets para discard ride dialog.
/// Cobre cenários como: aviso de descartar, quando o X pergunta antes de fechar.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  group('aviso de descartar', () {
    testWidgets('passageiro: "Descartar" confirma', (tester) async {
      final answer = await _openDialog(tester, isDriver: false);

      expect(find.text('Descartar o pedido?'), findsOneWidget);
      expect(
        find.text('O trajeto e o horário que você escolheu serão perdidos.'),
        findsOneWidget,
      );

      await tester.tap(find.byKey(const ValueKey('discard_ride_confirm')));
      await tester.pumpAndSettle();

      expect(answer(), isTrue);
    });

    testWidgets('motorista: o aviso fala em carona', (tester) async {
      await _openDialog(tester, isDriver: true);

      expect(find.text('Descartar a carona?'), findsOneWidget);
      expect(find.text('Descartar o pedido?'), findsNothing);
    });

    testWidgets('"Continuar" mantém o pedido', (tester) async {
      final answer = await _openDialog(tester, isDriver: false);

      await tester.tap(find.byKey(const ValueKey('discard_ride_keep')));
      await tester.pumpAndSettle();

      expect(answer(), isFalse);
    });

    testWidgets('tocar fora do aviso também mantém o pedido', (tester) async {
      final answer = await _openDialog(tester, isDriver: false);

      await tester.tapAt(const Offset(4, 4));
      await tester.pumpAndSettle();

      expect(answer(), isFalse);
      expect(find.text('Descartar o pedido?'), findsNothing);
    });
  });

  group('quando o X pergunta antes de fechar', () {
    test('na primeira etapa ainda não há o que perder', () {
      expect(PassengerRideStep.idle.hasConfirmedPickup, isFalse);
      expect(PassengerRideStep.selectPickup.hasConfirmedPickup, isFalse);
      expect(PassengerRideStep.dragMapPickup.hasConfirmedPickup, isFalse);
    });

    test('depois de confirmar a partida, pergunta', () {
      for (final step in [
        PassengerRideStep.searchDestination,
        PassengerRideStep.dragMapDestination,
        PassengerRideStep.routePreview,
        PassengerRideStep.routeConfirmation,
        PassengerRideStep.offerDetails,
      ]) {
        expect(step.hasConfirmedPickup, isTrue, reason: '$step');
      }
    });
  });
}
