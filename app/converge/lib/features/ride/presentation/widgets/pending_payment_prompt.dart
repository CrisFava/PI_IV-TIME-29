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

/// Estado do observador: se o painel de pagamento já está aberto.
class _PendingPaymentPromptState extends ConsumerState<PendingPaymentPrompt> {
  /// Painel aberto agora; evita abrir dois por cima um do outro.
  bool _open = false;

  /// Ouve as cobranças em aberto e o estado do painel, e não desenha nada.
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

  /// Abre o painel com a próxima cobrança a mostrar, se houver e se nenhum estiver aberto. Depois que o
  /// painel fecha, marca a cobrança como dispensada (fechou sem pagar) ou confere se chegou outra.
  Future<void> _maybeShow(AsyncValue<List<RidePaymentModel>> payments) async {
    // Recarregando, o valor ainda é o antigo: depois do "Já paguei", ele ainda traz a
    // cobrança que acabou de ser paga, e o pagamento abriria de novo. Com erro na
    // consulta, o valor também é o antigo: espera a próxima consulta dar certo.
    final pending = payments.value;
    if (_open || payments.isLoading || payments.hasError || pending == null) {
      return;
    }

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
    // Pagou: uma cobrança que chegou com o pagamento aberto foi ignorada; confere de novo
    _maybeShow(ref.read(pendingPaymentsProvider));
  }
}
