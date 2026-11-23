// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../ride/data/models/payment_status.dart';
import '../../../ride/data/models/ride_payment_model.dart';
import '../../../ride/presentation/widgets/payment_prompt_sheet.dart';
import '../../../ride/state/ride_providers.dart';
import '../../data/models/chat_conversation.dart';
import '../../state/chat_providers.dart';
import 'chat_panel_button.dart';
import 'chat_panel_card.dart';

/// Faixa de pagamento da conversa.
///
/// A cobrança é emitida sozinha no fim da viagem do passageiro, com o valor do rateio.
/// Motorista: acompanha se o passageiro já pagou.
/// Passageiro: o valor em aberto e o "Pagar", que abre o mesmo pagamento da Home.
///
/// Sem cobrança aplicável (ausente, viagem sem custo), a faixa não aparece.
class ChatPaymentPanel extends ConsumerWidget {
  /// Conversa cujo pagamento é mostrado.
  final ChatConversation conversation;

  const ChatPaymentPanel({super.key, required this.conversation});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final thread = conversation.thread;
    final payment = ref.watch(ridePaymentProvider(thread.passenger));

    return payment.when(
      loading: () => const LinearProgressIndicator(
        minHeight: 2,
        color: AppColors.primary,
        backgroundColor: AppColors.progressTrack,
      ),
      error: (error, _) => ChatPanelCard(
        icon: Icons.error_outline_rounded,
        title: 'Não foi possível carregar o pagamento.',
        subtitle: chatErrorMessage(error),
        actions: [
          ChatPanelButton(
            label: 'Tentar novamente',
            icon: Icons.refresh_rounded,
            primary: false,
            onPressed: () =>
                ref.invalidate(ridePaymentProvider(thread.passenger)),
          ),
        ],
      ),
      data: (payment) {
        if (payment.status == PaymentStatus.notApplicable) {
          return const SizedBox.shrink();
        }
        if (payment.isPaid) return _paid(payment);

        return conversation.viewerIsDriver
            ? _driver(payment)
            : _passenger(context, payment);
      },
    );
  }

  /// Pagamento já informado pelo passageiro ("Já paguei"), com o texto de cada lado.
  Widget _paid(RidePaymentModel payment) {
    final amount = payment.amount;
    return ChatPanelCard(
      icon: Icons.check_circle_rounded,
      success: true,
      title: amount == null
          ? 'Pagamento informado'
          : 'Pagamento de ${CurrencyFormatter.format(amount)} informado',
      subtitle: conversation.viewerIsDriver
          ? 'O passageiro informou que pagou. Confira o PIX na sua conta.'
          : 'Você informou o pagamento desta carona.',
    );
  }

  /// Visão do motorista. Ele só acompanha: a cobrança é emitida sozinha, com o valor do rateio.
  Widget _driver(RidePaymentModel payment) {
    final amount = payment.amount;

    if (amount == null) {
      return const ChatPanelCard(
        icon: Icons.pix_rounded,
        title: 'Pagamento',
        subtitle: 'O passageiro recebe a cobrança do rateio pelo PIX quando você confirma a chegada dele ou encerra a viagem.',
      );
    }

    return ChatPanelCard(
      icon: Icons.pix_rounded,
      title: 'Cobrança de ${CurrencyFormatter.format(amount)} enviada',
      subtitle: 'Aguardando o passageiro pagar pelo PIX.',
    );
  }

  /// Visão do passageiro: antes da cobrança, explica quando ela vem; depois, mostra o valor em aberto,
  /// o aviso de bloqueio (RF74) e o botão "Pagar", que abre a folha com o PIX.
  Widget _passenger(BuildContext context, RidePaymentModel payment) {
    final amount = payment.amount;

    if (amount == null) {
      return const ChatPanelCard(
        icon: Icons.pix_rounded,
        title: 'Pagamento',
        subtitle: 'O valor do rateio é cobrado pelo PIX quando a sua viagem terminar.',
      );
    }

    return ChatPanelCard(
      icon: Icons.pix_rounded,
      title: 'Pagamento de ${CurrencyFormatter.format(amount)} em aberto',
      subtitle:
          'Pague ${payment.receiverName} pelo PIX. Enquanto não pagar, você não pode pedir nem oferecer outra carona.',
      actions: [
        ChatPanelButton(
          label: 'Pagar',
          icon: Icons.pix_rounded,
          onPressed: () {
            HapticFeedback.lightImpact();
            showPaymentPromptSheet(context, payment);
          },
        ),
      ],
    );
  }
}
