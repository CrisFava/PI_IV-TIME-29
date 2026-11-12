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

final chatActionsProvider = NotifierProvider.autoDispose
    .family<ChatActionsController, AsyncValue<void>, ChatThreadRef>(
      ChatActionsController.new,
    );

/// Ações de uma conversa: enviar mensagem, cobrar e confirmar o pagamento,
/// iniciar e concluir a viagem.
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

  /// "Solicitar pagamento": registra a cobrança do valor combinado e avisa o passageiro.
  Future<bool> requestPayment(double amount) => _run(() async {
    final payment = await _rides.requestPayment(
      thread.rideId,
      thread.passengerId,
      amount: amount,
    );
    _refreshPayment();

    await _notify(
      thread,
      ChatMessageType.paymentRequest,
      'Pagamento solicitado.',
      amount: payment.amount ?? amount,
    );
  });

  /// "Passageiro já pagou": confirma o recebimento e avisa o passageiro.
  Future<bool> confirmPayment() => _run(() async {
    final payment = await _rides.confirmPayment(
      thread.rideId,
      thread.passengerId,
    );
    _refreshPayment();

    await _notify(
      thread,
      ChatMessageType.paymentConfirmed,
      'Pagamento confirmado.',
      amount: payment.amount,
    );
  });

  Future<bool> startRide() => _run(() async {
    final ride = await _rides.startRide(thread.rideId);
    _refreshRide();

    await _announce(ride, 'Viagem iniciada.');
  });

  /// Conclui a viagem no desembarque. A partir daqui os dois podem se avaliar.
  Future<bool> completeRide() => _run(() async {
    final ride = await _rides.completeRide(thread.rideId);
    _refreshRide();

    await _announce(ride, 'Viagem concluída. Avalie como foi a carona.');
  });

  void _refreshPayment() {
    if (ref.mounted) ref.invalidate(ridePaymentProvider(thread.passenger));
  }

  void _refreshRide() {
    if (!ref.mounted) return;
    ref.invalidate(rideProvider(thread.rideId));
    ref.invalidate(myRidesProvider);
  }

  /// Avisa todos os passageiros com vaga, cada um na sua conversa com o motorista.
  Future<void> _announce(RideModel ride, String text) async {
    final passengerIds = {
      thread.passengerId,
      for (final passenger in ride.passengersWithSeat) passenger.userId!,
    };

    for (final passengerId in passengerIds) {
      await _notify(
        ChatThreadRef(
          chatRoomId: thread.chatRoomId,
          rideId: thread.rideId,
          passengerId: passengerId,
        ),
        ChatMessageType.system,
        text,
      );
    }
  }

  /// Aviso de uma mudança que já foi gravada na API. Se o chat falhar aqui, a
  /// ação continua valendo: o outro participante vê o novo estado ao reabrir a conversa.
  Future<void> _notify(
    ChatThreadRef target,
    ChatMessageType type,
    String text, {
    double? amount,
  }) async {
    try {
      await _send(target, type, text, amount: amount);
    } on Exception {
      // Sem o aviso em tempo real; o estado na API já está correto.
    }
  }

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
