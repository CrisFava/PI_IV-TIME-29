// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/chat/data/models/chat_message_type.dart';
import 'package:converge/features/chat/data/models/chat_message_model.dart';
import 'package:converge/features/chat/state/ride_chat_announcer.dart';
import 'package:converge/features/chat/data/models/chat_thread_ref.dart';
import 'package:converge/features/chat/data/repository/chat_exception.dart';
import 'package:converge/features/chat/data/repository/firebase_chat_repository.dart';
import 'package:converge/features/chat/state/chat_actions_controller.dart';
import 'package:converge/features/chat/state/chat_providers.dart';
import 'package:converge/features/ride/data/models/ride_status.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import '../ride/fakes/fake_ride_repository.dart';
import 'fakes/fake_chat_repository.dart';
import 'fakes/fake_chat_user.dart';

void main() {
  const thread = ChatThreadRef(
    chatRoomId: 'ride_ride-1',
    rideId: fakeRideId,
    driverId: fakeDriverId,
    passengerId: fakePassengerId,
  );

  late FakeRideRepository rides;
  late FakeChatRepository chat;
  late ProviderContainer container;

  ChatActionsController controller() =>
      container.read(chatActionsProvider(thread).notifier);

  setUp(() {
    rides = FakeRideRepository();
    chat = FakeChatRepository();
    container = ProviderContainer.test(
      overrides: [
        rideRepositoryProvider.overrideWithValue(rides),
        chatRepositoryProvider.overrideWithValue(chat),
        chatViewerProvider.overrideWithValue(
          fakeChatUser(id: fakeDriverId, name: 'Lucas Silveira'),
        ),
      ],
    );
    // Mantém o controller vivo durante o teste (o provider é autoDispose).
    container.listen(chatActionsProvider(thread), (_, _) {});
  });

  tearDown(() => chat.dispose());

  group('sendText', () {
    test('envia a mensagem em nome do usuário logado', () async {
      final sent = await controller().sendText('  Chego em 5 minutos  ');

      expect(sent, isTrue);
      final message = chat.messagesOf(thread).single;
      expect(message.type, ChatMessageType.text);
      expect(message.text, 'Chego em 5 minutos');
      expect(message.senderId, fakeDriverId);
      expect(message.senderName, 'Lucas Silveira');
    });

    test('ignora mensagem em branco', () async {
      expect(await controller().sendText('   '), isFalse);
      expect(chat.messagesOf(thread), isEmpty);
    });

    test('falha do chat deixa o erro no estado para a tela exibir', () async {
      chat.failOnSend = true;

      final sent = await controller().sendText('Oi');

      expect(sent, isFalse);
      final state = container.read(chatActionsProvider(thread));
      expect(state.hasError, isTrue);
      expect(state.error, isA<ChatException>());
      expect(
        chatErrorMessage(state.error!),
        'Não foi possível enviar a mensagem.',
      );
    });
  });

  group('viagem', () {
    test('iniciar avisa o passageiro na conversa', () async {
      final started = await controller().startRide();

      expect(started, isTrue);
      expect(rides.ride.status, RideStatus.inProgress);
      final notice = chat.messagesOf(thread).single;
      expect(notice.type, ChatMessageType.system);
      expect(notice.text, 'Viagem iniciada.');
    });

    test('concluir avisa cada passageiro na sua própria conversa', () async {
      Map<String, dynamic> secondPassenger(String status) => {
        'userId': 'passenger-2',
        'name': 'Bruno Lima',
        'role': 'PASSENGER',
        'status': status,
      };
      rides.ride = fakeRide(
        status: 'IN_PROGRESS',
        extraParticipants: [secondPassenger('CONFIRMED')],
      );
      rides.completedRide = fakeRide(
        status: 'COMPLETED',
        passengerStatus: 'COMPLETED',
        extraParticipants: [secondPassenger('COMPLETED')],
      );
      const otherThread = ChatThreadRef(
        chatRoomId: 'ride_ride-1',
        rideId: fakeRideId,
        driverId: fakeDriverId,
        passengerId: 'passenger-2',
      );

      final completed = await controller().completeRide();

      expect(completed, isTrue);
      expect(rides.ride.status, RideStatus.completed);
      for (final passengerThread in [thread, otherThread]) {
        final notice = chat.messagesOf(passengerThread).single;
        expect(notice.type, ChatMessageType.system);
        expect(notice.text, 'Viagem concluída. Avalie como foi a carona.');
      }
    });

    test('se a API recusar concluir, ninguém é avisado', () async {
      rides.failNextActionWith =
          'Esta ação só vale para uma viagem em andamento.';

      final completed = await controller().completeRide();

      expect(completed, isFalse);
      expect(chat.messagesOf(thread), isEmpty);
    });
  });

  test('sem usuário logado, enviar mensagem falha com aviso claro', () async {
    final anonymous = ProviderContainer.test(
      overrides: [
        rideRepositoryProvider.overrideWithValue(rides),
        chatRepositoryProvider.overrideWithValue(chat),
        chatViewerProvider.overrideWithValue(null),
      ],
    );
    anonymous.listen(chatActionsProvider(thread), (_, _) {});

    final sent = await anonymous
        .read(chatActionsProvider(thread).notifier)
        .sendText('Oi');

    expect(sent, isFalse);
    expect(
      chatErrorMessage(anonymous.read(chatActionsProvider(thread)).error!),
      'Entre na sua conta para usar o chat.',
    );
  });

  group('RideChatAnnouncer', () {
    RideChatAnnouncer announcer() => RideChatAnnouncer(
      chat: chat,
      viewer: () => fakeChatUser(id: fakeDriverId, name: 'Lucas Silveira'),
    );

    test('alsoTo não duplica o aviso para passageiro com vaga', () async {
      await announcer().announce(
        fakeRide(),
        RideChatAnnouncer.rideStarted,
        alsoTo: fakePassengerId,
      );

      final message = chat.messagesOf(thread).single;
      expect(message.text, RideChatAnnouncer.rideStarted);
      expect(message.type, ChatMessageType.system);
      expect(message.senderId, fakeDriverId);
      expect(message.senderName, 'Lucas Silveira');
    });

    for (final status in ['REQUESTED', 'REJECTED', 'CANCELLED', 'NO_SHOW', 'EXPIRED']) {
      test('não avisa passageiro com status $status', () async {
        await announcer().announce(
          fakeRide(passengerStatus: status),
          RideChatAnnouncer.rideStarted,
        );

        expect(chat.messagesOf(thread), isEmpty);
      });
    }

    test('alsoTo recebe aviso mesmo sem vaga e usa sala informada', () async {
      const alternate = ChatThreadRef(
        chatRoomId: 'sala-alternativa',
        rideId: fakeRideId, driverId: fakeDriverId,
        passengerId: fakePassengerId,
      );
      await announcer().announce(
        fakeRide(chatRoomId: null, passengerStatus: 'CANCELLED'),
        RideChatAnnouncer.rideCompleted,
        chatRoomId: alternate.chatRoomId,
        alsoTo: fakePassengerId,
      );

      expect(chat.messagesOf(alternate).single.text, RideChatAnnouncer.rideCompleted);
      expect(chat.messagesOf(thread), isEmpty);
    });

    test('sem sala não tenta enviar anúncio nem cobrança', () async {
      final ride = fakeRide(chatRoomId: null);
      await announcer().announce(ride, RideChatAnnouncer.rideStarted);
      await announcer().notifyPassenger(
        ride, fakePassengerId, ChatMessageType.paymentRequest, 'Cobrança',
      );

      expect(chat.messagesOf(thread), isEmpty);
    });

    test('sem usuário não envia anúncio nem cobrança', () async {
      final anonymous = RideChatAnnouncer(chat: chat, viewer: () => null);
      await anonymous.announce(fakeRide(), RideChatAnnouncer.rideStarted);
      await anonymous.notifyPassenger(
        fakeRide(), fakePassengerId, ChatMessageType.paymentRequest, 'Cobrança',
      );

      expect(chat.messagesOf(thread), isEmpty);
    });

    test('cobrança leva o valor só para o passageiro indicado', () async {
      const otherThread = ChatThreadRef(
        chatRoomId: 'ride_ride-1',
        rideId: fakeRideId, driverId: fakeDriverId,
        passengerId: 'passenger-2',
      );
      await announcer().notifyPassenger(
        fakeRide(extraParticipants: [
          {'userId': 'passenger-2', 'role': 'PASSENGER', 'status': 'CONFIRMED'},
        ]),
        fakePassengerId,
        ChatMessageType.paymentRequest,
        'Cobrança da carona',
        amount: 12.5,
      );

      final message = chat.messagesOf(thread).single;
      expect(message.type, ChatMessageType.paymentRequest);
      expect(message.amount, 12.5);
      expect(message.text, 'Cobrança da carona');
      expect(chat.messagesOf(otherThread), isEmpty);
    });

    test('falha em uma conversa não impede avisar o próximo passageiro', () async {
      final flaky = _FailFirstChatRepository();
      addTearDown(flaky.dispose);
      final service = RideChatAnnouncer(
        chat: flaky,
        viewer: () => fakeChatUser(id: fakeDriverId, name: 'Lucas Silveira'),
      );
      await service.announce(
        fakeRide(extraParticipants: [
          {'userId': 'passenger-2', 'role': 'PASSENGER', 'status': 'CONFIRMED'},
        ]),
        RideChatAnnouncer.rideStarted,
      );

      expect(flaky.attemptedPassengers, [fakePassengerId, 'passenger-2']);
      expect(flaky.messagesOf(thread), isEmpty);
      expect(flaky.messagesOf(const ChatThreadRef(
        chatRoomId: 'ride_ride-1',
        rideId: fakeRideId, driverId: fakeDriverId,
        passengerId: 'passenger-2',
      )).single.text, RideChatAnnouncer.rideStarted);
    });

    test('falha ao enviar cobrança não propaga erro', () async {
      chat.failOnSend = true;

      await expectLater(
        announcer().notifyPassenger(
          fakeRide(), fakePassengerId, ChatMessageType.paymentRequest, 'Cobrança',
          amount: 12.5,
        ),
        completes,
      );
      expect(chat.messagesOf(thread), isEmpty);
    });
  });
}

class _FailFirstChatRepository extends FakeChatRepository {
  final attemptedPassengers = <String>[];

  @override
  Future<void> sendMessage(ChatThreadRef thread, ChatMessageModel message) async {
    attemptedPassengers.add(thread.passengerId);
    if (attemptedPassengers.length == 1) {
      throw const ChatException('Chat indisponível.');
    }
    await super.sendMessage(thread, message);
  }
}
