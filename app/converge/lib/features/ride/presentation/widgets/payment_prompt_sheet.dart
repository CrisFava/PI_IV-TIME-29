// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../chat/presentation/widgets/copy_pix_button.dart';
import '../../data/models/ride_payment_model.dart';
import '../../state/payment_prompt_controller.dart';
import 'pix_qr_code.dart';

/// Abre o pagamento de uma cobrança em aberto: o valor, o PIX e o "Já paguei".
/// O passageiro é levado a pagar na hora: não há "Depois" e o painel não fecha arrastando
/// nem tocando fora. Só fecha sem pagar quando o código PIX não está disponível.
/// Devolve `true` se o passageiro informou o pagamento (e mostra a confirmação).
Future<bool?> showPaymentPromptSheet(
  BuildContext context,
  RidePaymentModel payment,
) async {
  final canClose = payment.pixCopyPaste == null;
  final paid = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    isDismissible: canClose,
    enableDrag: canClose,
    backgroundColor: AppColors.cardBackground,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
    ),
    builder: (_) => PopScope(
      canPop: canClose,
      child: PaymentPromptSheet(payment: payment),
    ),
  );

  if (paid == true && context.mounted) {
    showAppNotice(
      context,
      'Pagamento informado. Você já pode pedir e oferecer caronas.',
      type: AppNoticeType.success,
    );
  }
  return paid;
}

/// Pagamento da carona: o passageiro copia o PIX, paga no banco e volta para o "Já paguei".
class PaymentPromptSheet extends ConsumerWidget {
  final RidePaymentModel payment;

  const PaymentPromptSheet({super.key, required this.payment});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final marking = ref.watch(paymentPromptProvider).isMarking;
    final amount = payment.amount;
    final pixCode = payment.pixCopyPaste;

    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(
        20,
        12,
        20,
        20 + MediaQuery.viewPaddingOf(context).bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Center(
            child: Container(
              width: 38,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.borderMedium,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 18),
          const Row(
            children: [
              Icon(Icons.pix_rounded, color: AppColors.primary),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Pague a carona',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            amount == null ? 'Valor a confirmar' : CurrencyFormatter.format(amount),
            style: const TextStyle(
              fontSize: 30,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            'para ${payment.receiverName}, o valor do rateio da viagem',
            style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
          ),
          const SizedBox(height: 16),
          if (pixCode != null) ...[
            PixQrCode(pixCode: pixCode),
            const SizedBox(height: 14),
          ],
          Text(
            pixCode == null
                ? 'O código PIX não está disponível agora. Tente de novo em instantes.'
                : 'Escaneie o QR Code ou copie o código, pague no app do seu banco e volte aqui para informar o pagamento.',
            style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary),
          ),
          if (pixCode != null) ...[
            const SizedBox(height: 12),
            CopyPixButton(pixCode: pixCode),
          ],
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.neutralLight,
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Text(
              'Enquanto não pagar, você não pode pedir nem oferecer outra carona.',
              style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w600,
                color: AppColors.textMedium,
              ),
            ),
          ),
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: marking ? null : () => _markAsPaid(context, ref),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              disabledBackgroundColor: AppColors.buttonSecondary,
              padding: const EdgeInsets.symmetric(vertical: 15),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            icon: marking
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.2,
                      color: AppColors.primary,
                    ),
                  )
                : const Icon(Icons.check_circle_rounded, size: 18),
            label: const Text(
              'Já paguei',
              style: TextStyle(fontSize: 14.5, fontWeight: FontWeight.w700),
            ),
          ),
          // Sem o código PIX não dá para pagar agora: só aí o painel pode ser fechado
          if (pixCode == null) ...[
            const SizedBox(height: 4),
            TextButton(
              onPressed: marking ? null : () => Navigator.of(context).pop(false),
              child: const Text(
                'Fechar',
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _markAsPaid(BuildContext context, WidgetRef ref) async {
    HapticFeedback.mediumImpact();
    final error = await ref
        .read(paymentPromptProvider.notifier)
        .markAsPaid(payment);
    if (!context.mounted) return;

    if (error != null) {
      showAppNotice(context, error, type: AppNoticeType.error);
      return;
    }
    Navigator.of(context).pop(true);
  }
}
