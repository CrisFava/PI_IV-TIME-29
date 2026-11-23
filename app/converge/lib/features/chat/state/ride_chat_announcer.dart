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

/// Anunciador dos avisos de viagem no chat, usado pelas telas do motorista.
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
  /// Texto do aviso de viagem iniciada.
  static const String rideStarted = 'Viagem iniciada.';

  /// Texto do aviso de viagem concluída, que também lembra de avaliar.
  static const String rideCompleted =
      'Viagem concluída. Avalie como foi a carona.';

  /// Repositório onde o aviso é gravado.
  final ChatRepository _chat;

  /// Quem envia o aviso (o usuário logado), lido na hora do envio.
  final UserModel? Function() _viewer;

  RideChatAnnouncer({required this._chat, required this._viewer});

  /// Manda [text] no grupo da viagem (início e fim da viagem). As cobranças não vão
  /// para o chat: cada passageiro vê a dele na tela de pagamento.
  ///
  /// Use [chatRoomId] quando a viagem recebida ainda não traz a sala. Sem sala ou sem usuário logado,
  /// não faz nada.
  Future<void> announce(
    RideModel ride,
    String text, {
    String? chatRoomId,
  }) async {
    final room = chatRoomId ?? ride.chatRoomId;
    final viewer = _viewer();
    if (room == null || viewer == null) return;

    await _send(
      room,
      ride.id,
      ride.driverId,
      viewer.id,
      viewer,
      ChatMessageType.system,
      text,
    );
  }

  /// Grava a mensagem como aviso do usuário logado. Qualquer erro é engolido: o aviso é só um extra.
  Future<void> _send(
    String room,
    String rideId,
    String driverId,
    String passengerId,
    UserModel viewer,
    ChatMessageType type,
    String text, {
    double? amount,
  }) async {
    try {
      await _chat.sendMessage(
        ChatThreadRef(
          chatRoomId: room,
          rideId: rideId,
          driverId: driverId,
          passengerId: passengerId,
        ),
        ChatMessageModel(
          senderId: viewer.id,
          senderName: viewer.name,
          type: type,
          text: text,
          amount: amount,
        ),
      );
    } on Exception {
      // Sem o aviso em tempo real; o passageiro vê o novo estado ao reabrir a conversa.
    }
  }
}
