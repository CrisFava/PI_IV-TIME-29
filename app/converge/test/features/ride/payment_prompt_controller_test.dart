// Autor: Cristian Eduardo Fava - RA 25000636

import 'dart:async';

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/ride/data/models/payment_status.dart';
import 'package:converge/features/ride/data/models/ride_payment_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/ride/state/payment_prompt_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_ride.dart';
import 'fakes/fake_ride_repository.dart';

/// Testes unitários e de widgets para payment prompt controller.
/// Cobre cenários como: mostra a cobrança em aberto até o passageiro deixar para depois, cobrança paga ou sem valor não aparece.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  late FakeRideRepository rides;
  late ProviderContainer container;

  // Controller do painel de pagamento no container do teste.
  PaymentPromptController controller() =>
      container.read(paymentPromptProvider.notifier);

  // Cobrança de R$ 12,50 emitida e ainda não paga.
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

  test('ignora cobranças fechadas e mantém a ordem das restantes', () {
    final second = RidePaymentModel.fromJson(const {
      'rideId': 'ride-2',
      'passengerId': fakePassengerId,
      'amount': 8,
      'status': 'PENDING',
    });
    final paid = FakeRideRepository.fakePayment(
      amount: 3,
      status: PaymentStatus.paid,
    );

    expect(controller().nextToShow([]), isNull);
    expect(controller().nextToShow([paid, open, second]), same(open));
    controller().dismiss(fakeRideId);
    expect(controller().nextToShow([paid, open, second]), same(second));
    controller().dismiss('ride-2');
    expect(controller().nextToShow([open, second]), isNull);
  });

  test('erro comum não apaga as cobranças fechadas', () {
    controller().dismiss(fakeRideId);

    expect(controller().handleBlockedAction(Exception('offline')), isFalse);
    expect(controller().nextToShow([open]), isNull);
  });

  test('falha ao pagar mantém a cobrança e permite tentar novamente', () async {
    rides.failNextActionWith = 'Tente novamente.';

    expect(await controller().markAsPaid(open), 'Tente novamente.');
    expect(controller().nextToShow(rides.pendingPayments), same(open));
    expect(await controller().markAsPaid(open), isNull);
    expect(
      rides.calls.where((call) => call == 'markPaymentAsPaid:$fakeRideId'),
      hasLength(2),
    );
    expect(container.read(paymentPromptProvider).markingRideId, isNull);
  });

  test('duplo toque envia uma única chamada e mantém o botão ocupado', () async {
    final delayed = _DelayedPaymentRepository();
    final scoped = ProviderContainer.test(
      overrides: [rideRepositoryProvider.overrideWithValue(delayed)],
    );
    final actions = scoped.read(paymentPromptProvider.notifier);
    final first = actions.markAsPaid(open);

    expect(scoped.read(paymentPromptProvider).markingRideId, fakeRideId);
    expect(await actions.markAsPaid(open), isNull);
    expect(delayed.requests, [(fakeRideId, fakePassengerId)]);
    expect(scoped.read(paymentPromptProvider).isMarking, isTrue);

    actions.dismiss('other-ride');
    expect(scoped.read(paymentPromptProvider).isMarking, isTrue);
    delayed.result.complete(open);
    expect(await first, isNull);
    expect(scoped.read(paymentPromptProvider).isMarking, isFalse);
    expect(scoped.read(paymentPromptProvider).dismissedRideIds, {'other-ride'});
  });

  test('PAYMENT_PENDING reabre cobranças sem liberar pagamento em andamento', () async {
    final delayed = _DelayedPaymentRepository();
    final scoped = ProviderContainer.test(
      overrides: [rideRepositoryProvider.overrideWithValue(delayed)],
    );
    final actions = scoped.read(paymentPromptProvider.notifier);
    actions.dismiss(fakeRideId);
    final first = actions.markAsPaid(open);

    expect(
      actions.handleBlockedAction(const ApiException(
        code: ApiErrorCode.paymentPending,
        message: 'Pagamento em aberto.',
        status: 422,
      )),
      isTrue,
    );
    expect(actions.nextToShow([open]), same(open));
    expect(scoped.read(paymentPromptProvider).markingRideId, fakeRideId);
    expect(await actions.markAsPaid(open), isNull);
    expect(delayed.requests, hasLength(1));

    delayed.result.complete(open);
    await first;
    expect(scoped.read(paymentPromptProvider).isMarking, isFalse);
  });
}

/// Repositório cujo "Já paguei" só responde quando o teste completar [result], para simular uma
/// requisição demorada e conferir o que acontece com cliques repetidos no meio dela.
class _DelayedPaymentRepository extends FakeRideRepository {
  /// Resposta do "Já paguei", completada pelo teste.
  final result = Completer<RidePaymentModel>();

  /// Cada "Já paguei" recebido (viagem e passageiro).
  final requests = <(String, String)>[];

  /// Registra o pedido e espera [result].
  @override
  Future<RidePaymentModel> markPaymentAsPaid(
    String rideId,
    String passengerId,
  ) {
    requests.add((rideId, passengerId));
    return result.future;
  }
}
