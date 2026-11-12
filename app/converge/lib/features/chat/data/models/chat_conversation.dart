// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/ride_model.dart';
import 'chat_thread_ref.dart';

/// Uma conversa liberada, vista pelo usuário logado.
@immutable
class ChatConversation {
  final ChatThreadRef thread;
  final RideModel ride;

  /// Nome de quem está do outro lado da conversa.
  final String otherPartyName;

  /// `true` quando o usuário logado é o motorista da viagem.
  final bool viewerIsDriver;

  const ChatConversation({
    required this.thread,
    required this.ride,
    required this.otherPartyName,
    required this.viewerIsDriver,
  });

  /// Conversas de uma viagem para [viewerId].
  ///
  /// O chat só existe depois do aceite: a viagem precisa ter sala (`chatRoomId`)
  /// e o passageiro precisa estar com a vaga confirmada. O motorista tem uma
  /// conversa com cada passageiro aceito; o passageiro tem uma com o motorista.
  static List<ChatConversation> ofRide(RideModel ride, String viewerId) {
    final chatRoomId = ride.chatRoomId;
    if (chatRoomId == null || chatRoomId.isEmpty) return const [];

    if (ride.isDrivenBy(viewerId)) {
      return [
        for (final passenger in ride.passengersWithSeat)
          ChatConversation(
            thread: ChatThreadRef(
              chatRoomId: chatRoomId,
              rideId: ride.id,
              passengerId: passenger.userId!,
            ),
            ride: ride,
            otherPartyName: passenger.displayName,
            viewerIsDriver: true,
          ),
      ];
    }

    final own = ride.passengerOf(viewerId);
    if (own == null || !own.status.hasSeat) return const [];

    return [
      ChatConversation(
        thread: ChatThreadRef(
          chatRoomId: chatRoomId,
          rideId: ride.id,
          passengerId: viewerId,
        ),
        ride: ride,
        otherPartyName: ride.driverName,
        viewerIsDriver: false,
      ),
    ];
  }
}
