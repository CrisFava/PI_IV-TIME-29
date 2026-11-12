// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/state/ride_providers.dart';
import '../../data/models/chat_conversation.dart';
import '../../state/chat_actions_controller.dart';
import '../../state/chat_providers.dart';
import '../widgets/chat_composer.dart';
import '../widgets/chat_message_list.dart';
import '../widgets/chat_notice.dart';
import '../widgets/chat_payment_panel.dart';
import '../widgets/chat_ride_panel.dart';
import '../widgets/chat_scaffold.dart';

/// Conversa em tempo real entre o motorista e um passageiro de uma viagem (RF27).
///
/// A conversa só abre depois que o motorista aceita o passageiro. Além das
/// mensagens, a tela concentra o pagamento e o encerramento da viagem.
class ChatPage extends ConsumerStatefulWidget {
  final String rideId;
  final String passengerId;

  const ChatPage({super.key, required this.rideId, required this.passengerId});

  @override
  ConsumerState<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends ConsumerState<ChatPage> {
  /// Último aviso de pagamento ou de viagem já tratado, para não recarregar à toa.
  String? _lastHandledEventId;

  @override
  Widget build(BuildContext context) {
    final viewer = ref.watch(chatViewerProvider);
    final ride = ref.watch(rideProvider(widget.rideId));

    return ride.when(
      loading: () => const ChatScaffold(
        title: 'Conversa',
        body: Center(
          child: CircularProgressIndicator(
            color: AppColors.primary,
            strokeWidth: 2.8,
          ),
        ),
      ),
      error: (error, _) => ChatScaffold(
        title: 'Conversa',
        body: ChatNotice(
          icon: Icons.error_outline_rounded,
          title: 'Não foi possível abrir a conversa',
          message: chatErrorMessage(error),
          actionLabel: 'Tentar novamente',
          onAction: () => ref.invalidate(rideProvider(widget.rideId)),
        ),
      ),
      data: (ride) {
        final conversation = viewer == null
            ? null
            : ChatConversation.ofRide(ride, viewer.id)
                  .where((c) => c.thread.passengerId == widget.passengerId)
                  .firstOrNull;

        if (viewer == null || conversation == null) {
          return const ChatScaffold(
            title: 'Conversa',
            body: ChatNotice(
              icon: Icons.lock_rounded,
              title: 'Chat ainda não liberado',
              message: 'A conversa é liberada quando o motorista aceita o pedido de carona.',
            ),
          );
        }

        return _buildConversation(conversation, viewer.id);
      },
    );
  }

  Widget _buildConversation(ChatConversation conversation, String viewerId) {
    final thread = conversation.thread;
    final messages = ref.watch(chatMessagesProvider(thread));
    final busy = ref.watch(chatActionsProvider(thread)).isLoading;

    // Um aviso do outro participante (cobrança, pagamento, início ou fim da
    // viagem) significa que o estado mudou na API: recarrega o que a tela mostra.
    ref.listen(chatMessagesProvider(thread), (previous, next) {
      final latest = next.value?.lastOrNull;
      if (latest == null || !latest.type.changesRideState) return;
      if (latest.id == _lastHandledEventId) return;

      _lastHandledEventId = latest.id;
      if (previous?.value == null || latest.isFrom(viewerId)) return;

      ref.invalidate(ridePaymentProvider(thread.passenger));
      ref.invalidate(rideProvider(thread.rideId));
    });

    ref.listen(chatActionsProvider(thread), (previous, next) {
      if (next.hasError && !next.isLoading) _showError(next.error!);
    });

    return ChatScaffold(
      title: conversation.otherPartyName,
      subtitle: conversation.viewerIsDriver ? 'Passageiro' : 'Motorista',
      body: Column(
        children: [
          ChatRidePanel(conversation: conversation),
          ChatPaymentPanel(conversation: conversation),
          Expanded(
            child: messages.when(
              loading: () => const Center(
                child: CircularProgressIndicator(
                  color: AppColors.primary,
                  strokeWidth: 2.8,
                ),
              ),
              error: (error, _) => ChatNotice(
                icon: Icons.cloud_off_rounded,
                title: 'Chat indisponível',
                message: chatErrorMessage(error),
                actionLabel: 'Tentar novamente',
                onAction: () => ref.invalidate(chatMessagesProvider(thread)),
              ),
              data: (messages) =>
                  ChatMessageList(messages: messages, viewerId: viewerId),
            ),
          ),
          ChatComposer(
            enabled: !busy,
            onSend: ref.read(chatActionsProvider(thread).notifier).sendText,
          ),
        ],
      ),
    );
  }

  void _showError(Object error) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            chatErrorMessage(error),
            style: const TextStyle(
              fontWeight: FontWeight.w600,
              color: AppColors.textLight,
            ),
          ),
          backgroundColor: AppColors.error,
          behavior: SnackBarBehavior.floating,
        ),
      );
  }
}
