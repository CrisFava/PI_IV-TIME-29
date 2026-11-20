// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/models/user_model.dart';
import '../../ride/data/models/ride_model.dart';
import '../../ride/data/repository/ride_repository.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import '../../ride/state/ride_providers.dart';
import '../data/models/chat_message_model.dart';
import '../data/models/chat_message_type.dart';
import '../data/models/chat_thread_ref.dart';
import '../data/repository/chat_exception.dart';
import '../data/repository/chat_repository.dart';
import '../data/repository/firebase_chat_repository.dart';
import 'chat_providers.dart';
import 'ride_chat_announcer.dart';

final chatActionsProvider = NotifierProvider.autoDispose
    .family<ChatActionsController, AsyncValue<void>, ChatThreadRef>(
      ChatActionsController.new,
    );

/// Ações de uma conversa: enviar mensagem, iniciar e concluir a viagem.
/// A cobrança é emitida pela API no fim da viagem de cada passageiro, sem ação no chat.
///
/// A fonte de verdade do pagamento e da viagem é a API. O chat recebe só um
/// aviso de cada mudança, e é esse aviso que faz a tela do outro participante
/// se atualizar em tempo real.
class ChatActionsController extends Notifier<AsyncValue<void>> {
  ChatActionsController(this.thread);

  final ChatThreadRef thread;

  RideRepository get _rides => ref.read(rideRepositoryProvider);
  ChatRepository get _chat => ref.read(chatRepositoryProvider);

  @override
  AsyncValue<void> build() => const AsyncData(null);

  Future<bool> sendText(String text) {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return Future.value(false);

    return _run(() => _send(thread, ChatMessageType.text, trimmed));
  }

  Future<bool> startRide() => _run(() async {
    final ride = await _rides.startRide(thread.rideId);
    _refreshRide();

    await _announce(ride, RideChatAnnouncer.rideStarted);
  });

  /// Conclui a viagem no desembarque. A partir daqui os dois podem se avaliar.
  Future<bool> completeRide() => _run(() async {
    final ride = await _rides.completeRide(thread.rideId);
    _refreshRide();
    // Concluir já emite a cobrança do passageiro
    _refreshPayment();

    await _announce(ride, RideChatAnnouncer.rideCompleted);
  });

  void _refreshPayment() {
    if (ref.mounted) ref.invalidate(ridePaymentProvider(thread.passenger));
  }

  void _refreshRide() {
    if (!ref.mounted) return;
    ref.invalidate(rideProvider(thread.rideId));
    ref.invalidate(myRidesProvider);
  }

  /// Avisa todos no grupo da viagem.
  Future<void> _announce(RideModel ride, String text) => ref
      .read(rideChatAnnouncerProvider)
      .announce(ride, text, chatRoomId: thread.chatRoomId);

  Future<void> _send(
    ChatThreadRef target,
    ChatMessageType type,
    String text, {
    double? amount,
  }) {
    final UserModel? viewer = ref.read(chatViewerProvider);
    if (viewer == null) {
      throw const ChatException('Entre na sua conta para usar o chat.');
    }

    return _chat.sendMessage(
      target,
      ChatMessageModel(
        senderId: viewer.id,
        senderName: viewer.name,
        type: type,
        text: text,
        amount: amount,
      ),
    );
  }

  /// Ignora novas ações enquanto uma estiver em andamento.
  /// Retorna `true` em caso de sucesso.
  Future<bool> _run(Future<void> Function() action) async {
    if (state.isLoading) return false;
    state = const AsyncLoading();
    final result = await AsyncValue.guard(action);
    if (ref.mounted) state = result;
    return !result.hasError;
  }
}
