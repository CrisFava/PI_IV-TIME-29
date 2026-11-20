// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/ride_passenger_ref.dart';

/// Identifica a conversa de uma viagem.
///
/// A conversa é **um grupo por viagem**: o motorista e todos os passageiros
/// confirmados falam no mesmo lugar. [passengerId] é só o contexto de quem olha
/// (o passageiro vê o próprio pagamento no painel); não muda o caminho.
@immutable
class ChatThreadRef {
  final String chatRoomId;
  final String rideId;
  final String driverId;
  final String passengerId;

  const ChatThreadRef({
    required this.chatRoomId,
    required this.rideId,
    required this.driverId,
    required this.passengerId,
  });

  RidePassengerRef get passenger => (rideId: rideId, passengerId: passengerId);

  /// Caminho da conversa no Realtime Database: o grupo da sala da viagem.
  String get path => 'chats/$chatRoomId/group';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ChatThreadRef &&
          chatRoomId == other.chatRoomId &&
          rideId == other.rideId &&
          driverId == other.driverId &&
          passengerId == other.passengerId;

  @override
  int get hashCode => Object.hash(chatRoomId, rideId, driverId, passengerId);
}
