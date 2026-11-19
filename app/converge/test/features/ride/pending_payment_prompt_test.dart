// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/ride/presentation/widgets/pending_payment_prompt.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_ride.dart';
import 'fakes/fake_ride_repository.dart';

void main() {
  late FakeRideRepository rides;

  Future<void> openHome(WidgetTester tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [rideRepositoryProvider.overrideWithValue(rides)],
        child: const MaterialApp(
          home: Scaffold(body: PendingPaymentPrompt()),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    rides = FakeRideRepository()
      ..pendingPayments = [FakeRideRepository.fakePayment(amount: 12.5)];
  });

  testWidgets('com cobrança em aberto, o pagamento abre sozinho e o "Já paguei" fecha', (
    tester,
  ) async {
    await openHome(tester);

    expect(find.text('Pague a carona'), findsOneWidget);
    expect(find.textContaining('Lucas Silveira'), findsOneWidget);

    await tester.tap(find.text('Já paguei'));
    await tester.pumpAndSettle();

    expect(rides.calls, contains('markPaymentAsPaid:$fakeRideId'));
    expect(find.text('Pague a carona'), findsNothing);
  });

  testWidgets('"Depois" fecha o pagamento e ele não volta sozinho', (tester) async {
    await openHome(tester);

    await tester.tap(find.text('Depois'));
    await tester.pumpAndSettle();

    expect(find.text('Pague a carona'), findsNothing);
    expect(rides.calls, isNot(contains('markPaymentAsPaid:$fakeRideId')));
  });

  testWidgets('sem cobrança em aberto, nada abre', (tester) async {
    rides.pendingPayments = [];

    await openHome(tester);

    expect(find.text('Pague a carona'), findsNothing);
  });
}
