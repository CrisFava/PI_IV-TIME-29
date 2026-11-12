// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/chat/data/models/chat_message_type.dart';
import 'package:converge/features/chat/data/models/chat_thread_ref.dart';
import 'package:converge/features/chat/data/repository/chat_exception.dart';
import 'package:converge/features/chat/data/repository/firebase_chat_repository.dart';
import 'package:converge/features/chat/state/chat_actions_controller.dart';
import 'package:converge/features/chat/state/chat_providers.dart';
import 'package:converge/features/ride/data/models/payment_status.dart';
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
      expect(chatErrorMessage(state.error!), 'Não foi possível enviar a mensagem.');
    });
  });

  group('requestPayment', () {
    test('registra a cobrança na API e avisa o passageiro no chat', () async {
      final requested = await controller().requestPayment(12.5);

      expect(requested, isTrue);
      expect(rides.calls, contains('requestPayment:12.5'));

      final notice = chat.messagesOf(thread).single;
      expect(notice.type, ChatMessageType.paymentRequest);
      expect(notice.amount, 12.5);
      expect(notice.senderId, fakeDriverId);
    });

    test('se a API recusar, nada é enviado ao chat', () async {
      rides.failNextActionWith = 'O valor cobrado não pode passar do valor máximo.';

      final requested = await controller().requestPayment(999);

      expect(requested, isFalse);
      expect(chat.messagesOf(thread), isEmpty);
      expect(
        chatErrorMessage(container.read(chatActionsProvider(thread)).error!),
        'O valor cobrado não pode passar do valor máximo.',
      );
    });

    test('a cobrança vale mesmo se o aviso no chat falhar', () async {
      chat.failOnSend = true;

      final requested = await controller().requestPayment(12.5);

      expect(requested, isTrue);
      expect(rides.payment.amount, 12.5);
      expect(container.read(chatActionsProvider(thread)).hasError, isFalse);
    });
  });

  group('confirmPayment', () {
    test('marca como pago na API e avisa o passageiro no chat', () async {
      await controller().requestPayment(12.5);

      final confirmed = await controller().confirmPayment();

      expect(confirmed, isTrue);
      expect(rides.payment.status, PaymentStatus.paid);
      final notice = chat.messagesOf(thread).last;
      expect(notice.type, ChatMessageType.paymentConfirmed);
      expect(notice.amount, 12.5);
    });

    test('se a API recusar, o chat não recebe confirmação', () async {
      rides.failNextActionWith = 'Solicite o pagamento antes de confirmar o recebimento.';

      final confirmed = await controller().confirmPayment();

      expect(confirmed, isFalse);
      expect(chat.messagesOf(thread), isEmpty);
      expect(rides.payment.status, PaymentStatus.pending);
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
      rides.ride = fakeRide(
        status: 'IN_PROGRESS',
        extraParticipants: [
          {
            'userId': 'passenger-2',
            'name': 'Bruno Lima',
            'role': 'PASSENGER',
            'status': 'CONFIRMED',
          },
        ],
      );
      const otherThread = ChatThreadRef(
        chatRoomId: 'ride_ride-1',
        rideId: fakeRideId,
        passengerId: 'passenger-2',
      );

      final completed = await controller().completeRide();

      expect(completed, isTrue);
      expect(rides.ride.status, RideStatus.completed);
      expect(chat.messagesOf(thread).single.type, ChatMessageType.system);
      // O FakeRideRepository devolve a viagem concluída só com o primeiro passageiro.
      expect(chat.messagesOf(otherThread), isEmpty);
    });

    test('se a API recusar concluir, ninguém é avisado', () async {
      rides.failNextActionWith = 'Esta ação só vale para uma viagem em andamento.';

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
}
