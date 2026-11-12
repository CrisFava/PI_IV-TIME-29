// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/chat/data/models/chat_conversation.dart';
import 'package:converge/features/chat/data/models/chat_thread_ref.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';

void main() {
  group('ChatConversation.ofRide', () {
    test('motorista tem uma conversa com cada passageiro aceito', () {
      final ride = fakeRide(
        extraParticipants: [
          {
            'userId': 'passenger-2',
            'name': 'Bruno Lima',
            'role': 'PASSENGER',
            'status': 'CONFIRMED',
          },
          {
            'userId': 'passenger-3',
            'name': 'Carla Dias',
            'role': 'PASSENGER',
            'status': 'REQUESTED',
          },
        ],
      );

      final conversations = ChatConversation.ofRide(ride, fakeDriverId);

      expect(conversations.map((c) => c.thread.passengerId), [
        fakePassengerId,
        'passenger-2',
      ]);
      expect(conversations.map((c) => c.otherPartyName), [
        'Ana Souza',
        'Bruno Lima',
      ]);
      expect(conversations.every((c) => c.viewerIsDriver), isTrue);
    });

    test('passageiro aceito tem uma conversa com o motorista', () {
      final conversations = ChatConversation.ofRide(fakeRide(), fakePassengerId);

      expect(conversations, hasLength(1));
      expect(conversations.single.viewerIsDriver, isFalse);
      expect(conversations.single.otherPartyName, 'Lucas Silveira');
      expect(
        conversations.single.thread,
        const ChatThreadRef(
          chatRoomId: 'ride_ride-1',
          rideId: fakeRideId,
          passengerId: fakePassengerId,
        ),
      );
    });

    test('antes do aceite não existe conversa para ninguém', () {
      final ride = fakeRide(
        status: 'AWAITING_CONFIRMATION',
        chatRoomId: null,
        passengerStatus: 'REQUESTED',
      );

      expect(ChatConversation.ofRide(ride, fakeDriverId), isEmpty);
      expect(ChatConversation.ofRide(ride, fakePassengerId), isEmpty);
    });

    test('passageiro que ainda não foi aceito não entra na sala dos outros', () {
      final ride = fakeRide(
        extraParticipants: [
          {
            'userId': 'passenger-2',
            'name': 'Bruno Lima',
            'role': 'PASSENGER',
            'status': 'REQUESTED',
          },
        ],
      );

      expect(ChatConversation.ofRide(ride, 'passenger-2'), isEmpty);
    });

    test('passageiro recusado, ausente ou que saiu perde a conversa', () {
      for (final status in ['REJECTED', 'NO_SHOW', 'CANCELLED']) {
        final ride = fakeRide(passengerStatus: status);

        expect(
          ChatConversation.ofRide(ride, fakePassengerId),
          isEmpty,
          reason: status,
        );
        expect(ChatConversation.ofRide(ride, fakeDriverId), isEmpty, reason: status);
      }
    });

    test('a conversa continua disponível depois da viagem concluída', () {
      final ride = fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED');

      expect(ChatConversation.ofRide(ride, fakePassengerId), hasLength(1));
      expect(ChatConversation.ofRide(ride, fakeDriverId), hasLength(1));
    });

    test('quem não participa da viagem não tem conversa', () {
      expect(ChatConversation.ofRide(fakeRide(), 'desconhecido'), isEmpty);
    });
  });

  group('ChatThreadRef', () {
    test('cada passageiro tem o seu caminho dentro da sala da viagem', () {
      const thread = ChatThreadRef(
        chatRoomId: 'ride_abc',
        rideId: 'abc',
        passengerId: 'p1',
      );

      expect(thread.path, 'chats/ride_abc/p1');
      expect(thread.passenger, (rideId: 'abc', passengerId: 'p1'));
    });
  });
}
