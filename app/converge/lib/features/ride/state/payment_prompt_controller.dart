// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../auth/state/auth_action_controller.dart';
import '../data/models/ride_payment_model.dart';
import '../data/repository/ride_repository.dart';
import '../data/repository/ride_repository_impl.dart';
import 'payment_prompt_state.dart';
import 'pending_payments_provider.dart';
import 'ride_providers.dart';

/// Controle do painel de pagamento. Não é `autoDispose`: as cobranças dispensadas valem a sessão toda.
final paymentPromptProvider =
    NotifierProvider<PaymentPromptController, PaymentPromptState>(
      PaymentPromptController.new,
    );

/// Pagamento da carona para o passageiro: qual cobrança em aberto mostrar e o "Já paguei".
///
/// Enquanto houver cobrança em aberto, o passageiro não pede nem oferece carona (a API
/// recusa com PAYMENT_PENDING). Por isso o pagamento volta a aparecer sempre que uma
/// dessas ações é recusada, mesmo que ele o tenha fechado antes.
class PaymentPromptController extends Notifier<PaymentPromptState> {
  /// Chamadas de viagem e pagamento na API.
  RideRepository get _rides => ref.read(rideRepositoryProvider);

  /// Começa sem cobranças dispensadas e sem "Já paguei" em andamento.
  @override
  PaymentPromptState build() => const PaymentPromptState();

  /// A primeira cobrança em aberto que não foi fechada sem pagar nesta sessão.
  RidePaymentModel? nextToShow(List<RidePaymentModel> pending) {
    for (final payment in pending) {
      if (payment.isOpen && !state.dismissedRideIds.contains(payment.rideId)) {
        return payment;
      }
    }
    return null;
  }

  /// Fechou sem pagar (sem código PIX disponível): a cobrança não abre sozinha de novo nesta sessão.
  void dismiss(String rideId) {
    state = PaymentPromptState(
      dismissedRideIds: {...state.dismissedRideIds, rideId},
      markingRideId: state.markingRideId,
    );
  }

  /// Uma ação foi recusada porque há pagamento em aberto: mostra o pagamento de novo.
  /// Devolve `true` se era esse o motivo da recusa.
  ///
  /// Limpa as dispensas e recarrega as cobranças, o que faz o `PendingPaymentPrompt` abrir o painel.
  bool handleBlockedAction(Object error) {
    if (error is! ApiException || !error.isPaymentPending) return false;

    state = PaymentPromptState(markingRideId: state.markingRideId);
    ref.invalidate(pendingPaymentsProvider);
    return true;
  }

  /// "Já paguei": informa o pagamento e libera o passageiro. Devolve a mensagem de erro, ou `null`.
  ///
  /// Ignora um segundo envio enquanto o primeiro não termina. Em sucesso, recarrega as cobranças em
  /// aberto, o pagamento da viagem e a lista de viagens.
  Future<String?> markAsPaid(RidePaymentModel payment) async {
    if (state.isMarking) return null;
    state = PaymentPromptState(
      dismissedRideIds: state.dismissedRideIds,
      markingRideId: payment.rideId,
    );

    try {
      await _rides.markPaymentAsPaid(payment.rideId, payment.passengerId);
      ref.invalidate(pendingPaymentsProvider);
      ref.invalidate(
        ridePaymentProvider((
          rideId: payment.rideId,
          passengerId: payment.passengerId,
        )),
      );
      ref.invalidate(myRidesProvider);
      return null;
    } catch (e) {
      return authErrorMessage(e);
    } finally {
      state = PaymentPromptState(dismissedRideIds: state.dismissedRideIds);
    }
  }
}
