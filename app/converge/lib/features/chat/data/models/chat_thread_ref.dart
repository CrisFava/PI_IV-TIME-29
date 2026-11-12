// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/ride_passenger_ref.dart';

/// Identifica uma conversa: o motorista e um passageiro de uma viagem.
///
/// Cada passageiro aceito tem a sua conversa particular com o motorista,
/// dentro da sala da viagem (`chatRoomId`).
@immutable
class ChatThreadRef {
  final String chatRoomId;
  final String rideId;
  final String passengerId;

  const ChatThreadRef({
    required this.chatRoomId,
    required this.rideId,
    required this.passengerId,
  });

  RidePassengerRef get passenger => (rideId: rideId, passengerId: passengerId);

  /// Caminho da conversa no Realtime Database.
  String get path => 'chats/$chatRoomId/$passengerId';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ChatThreadRef &&
          chatRoomId == other.chatRoomId &&
          rideId == other.rideId &&
          passengerId == other.passengerId;

  @override
  int get hashCode => Object.hash(chatRoomId, rideId, passengerId);
}
