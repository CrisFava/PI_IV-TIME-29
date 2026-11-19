// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/ride_passenger_ref.dart';

/// Identifica uma conversa entre o motorista e um passageiro.
///
/// A conversa é do **par de pessoas**, não da viagem: as mensagens ficam no
/// mesmo lugar em todas as caronas entre os dois. A viagem ([rideId]) é só o
/// contexto da tela (pagamento, início e fim da viagem).
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

  /// Caminho da conversa no Realtime Database: o par de pessoas, em ordem
  /// (o mesmo para os dois, seja quem for o motorista em cada viagem).
  String get path {
    final pair = [driverId, passengerId]..sort();
    return 'chats/pair_${pair.first}/${pair.last}';
  }

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
