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
  /// Sala da viagem no Realtime Database (`ride_<rideId>`), criada pela API no aceite.
  final String chatRoomId;

  /// Id da viagem no MongoDB.
  final String rideId;

  /// Id do motorista da viagem.
  final String driverId;

  /// Id de quem está olhando a conversa (para o motorista, o próprio id).
  final String passengerId;

  const ChatThreadRef({
    required this.chatRoomId,
    required this.rideId,
    required this.driverId,
    required this.passengerId,
  });

  /// Referência ao pagamento do passageiro desta conversa, usada pelos providers de pagamento.
  RidePassengerRef get passenger => (rideId: rideId, passengerId: passengerId);

  /// Caminho da conversa no Realtime Database: o grupo da sala da viagem.
  String get path => 'chats/$chatRoomId/group';

  /// Igualdade por valor, porque a referência é chave de providers `family`.
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
