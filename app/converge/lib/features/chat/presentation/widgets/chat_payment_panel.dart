// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../ride/data/models/payment_status.dart';
import '../../../ride/data/models/ride_payment_model.dart';
import '../../../ride/state/ride_providers.dart';
import '../../data/models/chat_conversation.dart';
import '../../state/chat_actions_controller.dart';
import '../../state/chat_providers.dart';
import 'chat_panel_button.dart';
import 'chat_panel_card.dart';
import 'copy_pix_button.dart';
import 'request_payment_sheet.dart';

/// Faixa de pagamento da conversa.
///
/// Motorista: "Solicitar pagamento" e, depois, "Passageiro já pagou".
/// Passageiro: o valor cobrado e o botão para copiar o código PIX.
class ChatPaymentPanel extends ConsumerWidget {
  final ChatConversation conversation;

  const ChatPaymentPanel({super.key, required this.conversation});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final thread = conversation.thread;
    final payment = ref.watch(ridePaymentProvider(thread.passenger));
    final busy = ref.watch(chatActionsProvider(thread)).isLoading;

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
            ? _driver(context, ref, payment, busy)
            : _passenger(context, payment);
      },
    );
  }

  Widget _paid(RidePaymentModel payment) {
    final amount = payment.amount;
    return ChatPanelCard(
      icon: Icons.check_circle_rounded,
      success: true,
      title: amount == null
          ? 'Pagamento confirmado'
          : 'Pagamento de ${CurrencyFormatter.format(amount)} confirmado',
      subtitle: conversation.viewerIsDriver
          ? 'Você confirmou o recebimento.'
          : 'O motorista confirmou o recebimento.',
    );
  }

  Widget _driver(
    BuildContext context,
    WidgetRef ref,
    RidePaymentModel payment,
    bool busy,
  ) {
    final amount = payment.amount;

    if (amount == null) {
      return ChatPanelCard(
        icon: Icons.pix_rounded,
        title: 'Pagamento',
        subtitle: 'Combinem o valor no chat e depois solicite o pagamento.',
        actions: [
          ChatPanelButton(
            label: 'Solicitar pagamento',
            icon: Icons.request_quote_rounded,
            busy: busy,
            onPressed: () => _requestPayment(context, ref, null),
          ),
        ],
      );
    }

    return ChatPanelCard(
      icon: Icons.pix_rounded,
      title: 'Cobrança de ${CurrencyFormatter.format(amount)} enviada',
      subtitle: 'Confirme quando o PIX cair na sua conta.',
      actions: [
        ChatPanelButton(
          label: 'Passageiro já pagou',
          icon: Icons.check_rounded,
          busy: busy,
          onPressed: () => _confirmPayment(context, ref, amount),
        ),
        ChatPanelButton(
          label: 'Alterar valor',
          icon: Icons.edit_rounded,
          primary: false,
          busy: busy,
          onPressed: () => _requestPayment(context, ref, amount),
        ),
      ],
    );
  }

  Widget _passenger(BuildContext context, RidePaymentModel payment) {
    final amount = payment.amount;
    final pixCode = payment.pixCopyPaste;

    if (amount == null) {
      return const ChatPanelCard(
        icon: Icons.pix_rounded,
        title: 'Pagamento',
        subtitle:
            'Combinem o valor no chat. O motorista envia a cobrança por aqui.',
      );
    }

    return ChatPanelCard(
      icon: Icons.pix_rounded,
      title:
          '${payment.receiverName} solicitou ${CurrencyFormatter.format(amount)}',
      subtitle: pixCode == null
          ? 'O código PIX não está disponível no momento.'
          : 'Pague com o Pix Copia e Cola no app do seu banco. '
                'O motorista confirma o recebimento.',
      actions: [if (pixCode != null) CopyPixButton(pixCode: pixCode)],
    );
  }

  Future<void> _requestPayment(
    BuildContext context,
    WidgetRef ref,
    double? currentAmount,
  ) async {
    HapticFeedback.lightImpact();
    final amount = await showRequestPaymentSheet(
      context,
      passengerName: conversation.otherPartyName,
      initialAmount: currentAmount,
    );
    if (amount == null) return;

    await ref
        .read(chatActionsProvider(conversation.thread).notifier)
        .requestPayment(amount);
  }

  Future<void> _confirmPayment(
    BuildContext context,
    WidgetRef ref,
    double amount,
  ) async {
    HapticFeedback.lightImpact();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardBackground,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Confirmar recebimento',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
        content: Text(
          'Você recebeu ${CurrencyFormatter.format(amount)} de '
          '${conversation.otherPartyName}? Depois de confirmar, não dá para desfazer.',
          style: const TextStyle(fontSize: 14, color: AppColors.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text(
              'Ainda não',
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text(
              'Sim, já pagou',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    HapticFeedback.mediumImpact();
    await ref
        .read(chatActionsProvider(conversation.thread).notifier)
        .confirmPayment();
  }
}
