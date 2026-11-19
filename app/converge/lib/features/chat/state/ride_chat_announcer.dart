// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/models/user_model.dart';
import '../../ride/data/models/ride_model.dart';
import '../data/models/chat_message_model.dart';
import '../data/models/chat_message_type.dart';
import '../data/models/chat_thread_ref.dart';
import '../data/repository/chat_repository.dart';
import '../data/repository/firebase_chat_repository.dart';
import 'chat_providers.dart';

final rideChatAnnouncerProvider = Provider<RideChatAnnouncer>(
  (ref) => RideChatAnnouncer(
    chat: ref.watch(chatRepositoryProvider),
    viewer: () => ref.read(chatViewerProvider),
  ),
);

/// Avisa no chat de cada passageiro que a viagem mudou (iniciada, concluída).
///
/// A fonte de verdade é a API: o aviso só serve para a tela do passageiro se atualizar
/// na hora. Por isso vale para qualquer botão do motorista (chat, home ou painel de
/// caronas), e uma falha no chat não desfaz nem atrapalha a ação já gravada.
class RideChatAnnouncer {
  static const String rideStarted = 'Viagem iniciada.';
  static const String rideCompleted =
      'Viagem concluída. Avalie como foi a carona.';

  final ChatRepository _chat;
  final UserModel? Function() _viewer;

  RideChatAnnouncer({required this._chat, required this._viewer});

  /// Manda [text] para todos os passageiros com vaga e também para [alsoTo], se houver.
  Future<void> announce(
    RideModel ride,
    String text, {
    String? chatRoomId,
    String? alsoTo,
  }) async {
    final room = chatRoomId ?? ride.chatRoomId;
    final viewer = _viewer();
    if (room == null || viewer == null) return;

    final passengerIds = {
      ?alsoTo,
      for (final passenger in ride.passengersWithSeat) passenger.userId!,
    };

    for (final passengerId in passengerIds) {
      try {
        await _chat.sendMessage(
          ChatThreadRef(
            chatRoomId: room,
            rideId: ride.id,
            passengerId: passengerId,
          ),
          ChatMessageModel(
            senderId: viewer.id,
            senderName: viewer.name,
            type: ChatMessageType.system,
            text: text,
          ),
        );
      } on Exception {
        // Sem o aviso em tempo real; o passageiro vê o novo estado ao reabrir a conversa.
      }
    }
  }
}
