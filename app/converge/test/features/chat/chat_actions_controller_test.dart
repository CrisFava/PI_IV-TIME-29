// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/chat/data/models/chat_message_type.dart';
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

    test('avisa uma vez só, no grupo da viagem', () async {
      await announcer().announce(
        fakeRide(
          extraParticipants: [
            {
              'userId': 'passenger-2',
              'role': 'PASSENGER',
              'status': 'CONFIRMED',
            },
          ],
        ),
        RideChatAnnouncer.rideStarted,
      );

      final message = chat.messagesOf(thread).single;
      expect(message.text, RideChatAnnouncer.rideStarted);
      expect(message.type, ChatMessageType.system);
      expect(message.senderName, 'Lucas Silveira');
    });

    test('sem sala ou sem usuário, não envia nada', () async {
      await announcer().announce(
        fakeRide(chatRoomId: null),
        RideChatAnnouncer.rideStarted,
      );
      await RideChatAnnouncer(
        chat: chat,
        viewer: () => null,
      ).announce(fakeRide(), RideChatAnnouncer.rideStarted);

      expect(chat.messagesOf(thread), isEmpty);
    });

    test('falha ao enviar o aviso não propaga erro', () async {
      chat.failOnSend = true;

      await expectLater(
        announcer().announce(fakeRide(), RideChatAnnouncer.rideStarted),
        completes,
      );
    });
  });
}
