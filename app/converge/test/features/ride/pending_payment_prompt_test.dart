// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/ride/data/models/payment_status.dart';
import 'package:converge/features/ride/data/models/ride_payment_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/ride/presentation/widgets/pending_payment_prompt.dart';
import 'package:converge/features/ride/presentation/widgets/pix_qr_code.dart';
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
        child: const MaterialApp(home: Scaffold(body: PendingPaymentPrompt())),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    rides = FakeRideRepository()
      ..pendingPayments = [FakeRideRepository.fakePayment(amount: 12.5)];
  });

  testWidgets(
    'com cobrança em aberto, o pagamento abre sozinho e o "Já paguei" fecha',
    (tester) async {
      await openHome(tester);

      expect(find.text('Pague a carona'), findsOneWidget);
      expect(find.textContaining('Lucas Silveira'), findsOneWidget);
      // QR Code do mesmo Pix Copia e Cola, para pagar com outro celular
      expect(find.byType(PixQrCode), findsOneWidget);
      expect(find.bySemanticsLabel('QR Code do PIX'), findsOneWidget);

      await tester.tap(find.text('Já paguei'));
      await tester.pumpAndSettle();

      expect(rides.calls, contains('markPaymentAsPaid:$fakeRideId'));
      expect(find.text('Pague a carona'), findsNothing);
      expect(
        find.text(
          'Pagamento informado. Você já pode pedir e oferecer caronas.',
        ),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'sem "Depois": o pagamento não fecha tocando fora, só com o "Já paguei"',
    (tester) async {
      await openHome(tester);

      expect(find.text('Depois'), findsNothing);
      expect(find.text('Fechar'), findsNothing);
      await tester.tapAt(const Offset(400, 40));
      await tester.pumpAndSettle();

      expect(find.text('Pague a carona'), findsOneWidget);
    },
  );

  testWidgets('sem o código PIX, o pagamento pode ser fechado', (tester) async {
    // Cobrança em aberto, mas o código PIX não veio (ex.: falha ao gerar)
    rides.pendingPayments = [
      RidePaymentModel(
        rideId: fakeRideId,
        passengerId: fakePassengerId,
        driverId: fakeDriverId,
        receiverName: 'Lucas Silveira',
        amount: 12.5,
        status: PaymentStatus.pending,
        pixCopyPaste: null,
        requestedAt: DateTime(2026, 11, 16, 7),
        paidAt: null,
      ),
    ];
    await openHome(tester);

    await tester.tap(find.text('Fechar'));
    await tester.pumpAndSettle();

    expect(find.text('Pague a carona'), findsNothing);
  });

  testWidgets('sem cobrança em aberto, nada abre', (tester) async {
    rides.pendingPayments = [];

    await openHome(tester);

    expect(find.text('Pague a carona'), findsNothing);
  });
}
