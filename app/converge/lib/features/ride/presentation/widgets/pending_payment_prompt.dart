// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/ride_payment_model.dart';
import '../../state/payment_prompt_controller.dart';
import '../../state/pending_payments_provider.dart';
import 'payment_prompt_sheet.dart';

/// Abre o pagamento sozinho quando há cobrança em aberto: ao fim da viagem do passageiro,
/// ao abrir o app de novo e quando um pedido ou oferta é recusado por falta de pagamento.
/// Não desenha nada; fica na Home.
class PendingPaymentPrompt extends ConsumerStatefulWidget {
  const PendingPaymentPrompt({super.key});

  @override
  ConsumerState<PendingPaymentPrompt> createState() =>
      _PendingPaymentPromptState();
}

class _PendingPaymentPromptState extends ConsumerState<PendingPaymentPrompt> {
  bool _open = false;

  @override
  Widget build(BuildContext context) {
    ref.listen(pendingPaymentsProvider, (_, next) => _maybeShow(next));
    // Fechar sem pagar ou uma ação recusada por pagamento em aberto mudam o que deve aparecer
    ref.listen(
      paymentPromptProvider,
      (_, _) => _maybeShow(ref.read(pendingPaymentsProvider)),
    );
    return const SizedBox.shrink();
  }

  Future<void> _maybeShow(AsyncValue<List<RidePaymentModel>> payments) async {
    // Recarregando, o valor ainda é o antigo: depois do "Já paguei", ele ainda traz a
    // cobrança que acabou de ser paga, e o pagamento abriria de novo
    final pending = payments.value;
    if (_open || payments.isLoading || payments.hasError || pending == null) return;

    final payment = ref
        .read(paymentPromptProvider.notifier)
        .nextToShow(pending);
    if (payment == null) return;

    _open = true;
    final paid = await showPaymentPromptSheet(context, payment);
    _open = false;
    if (!mounted) return;

    // Só fecha sem pagar quando o código PIX não estava disponível: não reabre sozinho
    if (paid != true) {
      ref.read(paymentPromptProvider.notifier).dismiss(payment.rideId);
    } else {
      await _maybeShow(ref.read(pendingPaymentsProvider));
    }
  }
}
