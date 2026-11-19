// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/ride/data/models/payment_status.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/ride/state/payment_prompt_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_ride.dart';
import 'fakes/fake_ride_repository.dart';

void main() {
  late FakeRideRepository rides;
  late ProviderContainer container;

  PaymentPromptController controller() =>
      container.read(paymentPromptProvider.notifier);

  final open = FakeRideRepository.fakePayment(amount: 12.5);

  setUp(() {
    rides = FakeRideRepository()..pendingPayments = [open];
    container = ProviderContainer.test(
      overrides: [rideRepositoryProvider.overrideWithValue(rides)],
    );
  });

  test('mostra a cobrança em aberto até o passageiro deixar para depois', () {
    expect(controller().nextToShow([open])?.rideId, fakeRideId);

    controller().dismiss(fakeRideId);

    expect(controller().nextToShow([open]), isNull);
  });

  test('cobrança paga ou sem valor não aparece', () {
    final paid = FakeRideRepository.fakePayment(
      amount: 12.5,
      status: PaymentStatus.paid,
    );
    final notCharged = FakeRideRepository.fakePayment();

    expect(controller().nextToShow([paid, notCharged]), isNull);
  });

  test('pedido ou oferta recusado por pagamento em aberto mostra o pagamento de novo', () {
    controller().dismiss(fakeRideId);

    final handled = controller().handleBlockedAction(
      const ApiException(
        code: ApiErrorCode.paymentPending,
        message: 'Você tem o pagamento de uma carona em aberto.',
        status: 422,
      ),
    );

    expect(handled, isTrue);
    expect(controller().nextToShow([open])?.rideId, fakeRideId);
  });

  test('outros erros não reabrem o pagamento', () {
    controller().dismiss(fakeRideId);

    final handled = controller().handleBlockedAction(
      const ApiException(
        code: ApiErrorCode.badRequest,
        message: 'Esta viagem não tem mais vagas.',
        status: 400,
      ),
    );

    expect(handled, isFalse);
    expect(controller().nextToShow([open]), isNull);
  });

  test('"Já paguei" informa o pagamento na API e libera o botão', () async {
    final error = await controller().markAsPaid(open);

    expect(error, isNull);
    expect(rides.calls, contains('markPaymentAsPaid:$fakeRideId'));
    expect(rides.pendingPayments, isEmpty);
    expect(container.read(paymentPromptProvider).isMarking, isFalse);
  });

  test('se a API recusar o "Já paguei", devolve a mensagem', () async {
    rides.failNextActionWith = 'Não há cobrança para você nesta viagem.';

    final error = await controller().markAsPaid(open);

    expect(error, 'Não há cobrança para você nesta viagem.');
    expect(container.read(paymentPromptProvider).isMarking, isFalse);
  });
}
