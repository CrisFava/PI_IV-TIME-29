// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/ride_model.dart';
import 'chat_thread_ref.dart';

/// Uma conversa liberada, vista pelo usuário logado.
@immutable
class ChatConversation {
  final ChatThreadRef thread;
  final RideModel ride;

  /// Quem está do outro lado: o motorista (para o passageiro) ou os
  /// passageiros (para o motorista).
  final String otherPartyName;

  /// `true` quando o usuário logado é o motorista da viagem.
  final bool viewerIsDriver;

  const ChatConversation({
    required this.thread,
    required this.ride,
    required this.otherPartyName,
    required this.viewerIsDriver,
  });

  /// A conversa (em grupo) de uma viagem para [viewerId].
  ///
  /// O chat só existe depois do aceite: a viagem precisa ter sala (`chatRoomId`).
  /// O motorista entra com pelo menos um passageiro com vaga; o passageiro, com a
  /// vaga dele confirmada. Todos falam na mesma conversa.
  static List<ChatConversation> ofRide(RideModel ride, String viewerId) {
    final chatRoomId = ride.chatRoomId;
    if (chatRoomId == null || chatRoomId.isEmpty) return const [];

    final passengers = ride.passengersWithSeat;
    if (ride.isDrivenBy(viewerId)) {
      if (passengers.isEmpty) return const [];
      return [
        ChatConversation(
          thread: ChatThreadRef(
            chatRoomId: chatRoomId,
            rideId: ride.id,
            driverId: ride.driverId,
            passengerId: viewerId,
          ),
          ride: ride,
          otherPartyName: passengers.map((p) => p.displayName).join(', '),
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
          driverId: ride.driverId,
          passengerId: viewerId,
        ),
        ride: ride,
        otherPartyName: ride.driverName,
        viewerIsDriver: false,
      ),
    ];
  }
}
