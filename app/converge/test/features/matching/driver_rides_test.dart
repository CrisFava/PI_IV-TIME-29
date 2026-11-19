// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:converge/features/chat/data/models/chat_message_type.dart';
import 'package:converge/core/utils/currency_formatter.dart';
import 'package:converge/features/chat/data/models/chat_thread_ref.dart';
import 'package:converge/features/chat/data/repository/firebase_chat_repository.dart';
import 'package:converge/features/chat/state/chat_providers.dart';
import 'package:converge/features/matching/state/driver_ride_actions_controller.dart';
import 'package:converge/features/matching/state/driver_rides_provider.dart';
import 'package:converge/features/matching/state/matching_viewer_provider.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/misc.dart' show Override;
import 'package:flutter_test/flutter_test.dart';

import '../chat/fakes/fake_chat_repository.dart';
import '../chat/fakes/fake_chat_user.dart';
import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_seat_ride_repository.dart';

void main() {
  late FakeChatRepository chat;

  setUp(() => chat = FakeChatRepository());
  tearDown(() => chat.dispose());

  ProviderContainer containerWith(
    FakeSeatRideRepository rides, {
    String viewerId = fakeDriverId,
  }) {
    final container = ProviderContainer.test(
      overrides: [
        rideRepositoryProvider.overrideWithValue(rides),
        matchingViewerIdProvider.overrideWithValue(viewerId),
        ..._chatOverrides(chat),
      ],
    );
    container.listen(myDrivenRidesProvider, (_, _) {});
    container.listen(driverRideInProgressProvider, (_, _) {});
    container.listen(driverRideActionsProvider, (_, _) {});
    return container;
  }

  test('a viagem em andamento é o expediente', () async {
    final container = containerWith(
      FakeSeatRideRepository(myRides: [fakeRide(status: 'IN_PROGRESS')]),
    );

    await container.read(myDrivenRidesProvider.future);

    expect(container.read(driverRideInProgressProvider)?.id, fakeRideId);
  });

  test(
    'iniciar e concluir chamam a API; um erro volta como mensagem',
    () async {
      final rides = FakeSeatRideRepository(
        myRides: [fakeRide(status: 'CONFIRMED')],
      );
      final container = containerWith(rides);
      final actions = container.read(driverRideActionsProvider.notifier);

      expect(await actions.start(fakeRideId), isNull);
      expect(await actions.complete(fakeRideId), isNull);
      expect(
        rides.calls,
        containsAllInOrder(['start:$fakeRideId', 'complete:$fakeRideId']),
      );

      rides.failNextWith = 'Só é possível iniciar uma viagem confirmada.';
      expect(
        await actions.start(fakeRideId),
        'Só é possível iniciar uma viagem confirmada.',
      );
      expect(container.read(driverRideActionsProvider), isEmpty);
    },
  );

  test(
    '"Encerrar" na home conclui sem erro mesmo sem ninguém observando as ações',
    () async {
      // A home só lê o notifier: durante o expediente o painel que observa as ações está fechado
      final rides = _SlowRides(myRides: [fakeRide(status: 'IN_PROGRESS')]);
      final container = ProviderContainer.test(
        overrides: [
          rideRepositoryProvider.overrideWithValue(rides),
          matchingViewerIdProvider.overrideWithValue(fakeDriverId),
          ..._chatOverrides(chat),
        ],
      );

      final error = await container
          .read(driverRideActionsProvider.notifier)
          .complete(fakeRideId);

      expect(error, isNull);
      expect(rides.calls, contains('complete:$fakeRideId'));
    },
  );

  test('"Chegou" confirma a chegada e avisa só aquele passageiro da cobrança no chat', () async {
    final rides = FakeSeatRideRepository(
      myRides: [fakeRide(status: 'IN_PROGRESS')],
    );
    final container = containerWith(rides);
    const thread = ChatThreadRef(
      chatRoomId: 'ride_ride-1',
      rideId: fakeRideId, driverId: fakeDriverId,
      passengerId: fakePassengerId,
    );

    final error = await container
        .read(driverRideActionsProvider.notifier)
        .arrive(fakeRideId, fakePassengerId);

    expect(error, isNull);
    expect(rides.calls, contains('arrive:$fakeRideId:$fakePassengerId'));
    final notice = chat.messagesOf(thread).single;
    expect(notice.type, ChatMessageType.paymentRequest);
    expect(notice.text, startsWith('Você chegou.'));
  });

  test(
    'iniciar e concluir fora do chat também avisam o passageiro no chat',
    () async {
      final container = containerWith(
        FakeSeatRideRepository(myRides: [fakeRide(status: 'CONFIRMED')]),
      );
      final actions = container.read(driverRideActionsProvider.notifier);
      const thread = ChatThreadRef(
        chatRoomId: 'ride_ride-1',
        rideId: fakeRideId, driverId: fakeDriverId,
        passengerId: fakePassengerId,
      );

      await actions.start(fakeRideId);
      await actions.complete(fakeRideId);

      final messages = chat.messagesOf(thread);
      expect(messages.map((message) => message.type), [
        ChatMessageType.system,
        ChatMessageType.system,
      ]);
      expect(messages.map((message) => message.text), [
        'Viagem iniciada.',
        'Viagem concluída. Avalie como foi a carona.',
      ]);
    },
  );

  test('chegada recusada não envia cobrança e permite tentar novamente', () async {
    final rides = FakeSeatRideRepository()
      ..failNextWith = 'Passageiro não confirmado.';
    final container = containerWith(rides);
    final actions = container.read(driverRideActionsProvider.notifier);
    const thread = ChatThreadRef(
      chatRoomId: 'ride_ride-1', rideId: fakeRideId, driverId: fakeDriverId, passengerId: fakePassengerId,
    );

    expect(await actions.arrive(fakeRideId, fakePassengerId), 'Passageiro não confirmado.');
    expect(chat.messagesOf(thread), isEmpty);
    expect(container.read(driverRideActionsProvider), isEmpty);
    expect(await actions.arrive(fakeRideId, fakePassengerId), isNull);
    expect(chat.messagesOf(thread), hasLength(1));
  });

  test('falha no chat não transforma chegada gravada em erro', () async {
    chat.failOnSend = true;
    final rides = FakeSeatRideRepository();
    final container = containerWith(rides);

    expect(await container.read(driverRideActionsProvider.notifier)
        .arrive(fakeRideId, fakePassengerId), isNull);
    expect(rides.calls.where((call) => call.startsWith('arrive:')), hasLength(1));
    expect(container.read(driverRideActionsProvider), isEmpty);
  });

  for (final amount in <double?>[12.5, null]) {
    test('aviso de chegada usa valor final $amount devolvido pela API', () async {
      final rides = _ControlledArrivalRepository();
      final container = containerWith(rides);
      final result = container.read(driverRideActionsProvider.notifier)
          .arrive(fakeRideId, fakePassengerId);
      rides.arrival.complete(fakeRide(
        status: 'IN_PROGRESS',
        passengerStatus: 'CANCELLED',
        extraParticipants: [
          {
            'userId': fakePassengerId,
            'role': 'PASSENGER',
            'status': 'COMPLETED',
            'estimatedAmount': 20,
            'finalAmount': amount,
          },
        ],
      ));

      expect(await result, isNull);
      final notice = chat.messagesOf(const ChatThreadRef(
        chatRoomId: 'ride_ride-1', rideId: fakeRideId, driverId: fakeDriverId, passengerId: fakePassengerId,
      )).single;
      expect(notice.amount, amount);
      expect(notice.text, amount == null
          ? 'Você chegou. A cobrança da carona foi enviada.'
          : 'Você chegou. A cobrança de ${CurrencyFormatter.format(amount)} foi enviada.');
    });
  }

  test('duplo toque em Chegou não cobra duas vezes', () async {
    final rides = _ControlledArrivalRepository();
    final container = containerWith(rides);
    final actions = container.read(driverRideActionsProvider.notifier);
    final first = actions.arrive(fakeRideId, fakePassengerId);

    expect(container.read(driverRideActionsProvider), {fakeRideId});
    expect(await actions.arrive(fakeRideId, fakePassengerId), isNull);
    expect(rides.arrivalCalls, 1);
    expect(container.read(driverRideActionsProvider), {fakeRideId});

    rides.arrival.complete(fakeRide(status: 'IN_PROGRESS', passengerStatus: 'COMPLETED'));
    expect(await first, isNull);
    expect(container.read(driverRideActionsProvider), isEmpty);
  });
}

List<Override> _chatOverrides(FakeChatRepository chat) => [
  chatRepositoryProvider.overrideWithValue(chat),
  chatViewerProvider.overrideWithValue(
    fakeChatUser(id: fakeDriverId, name: 'Lucas Silveira'),
  ),
];

/// A API demora um pouco para responder, como numa rede de verdade.
class _SlowRides extends FakeSeatRideRepository {
  _SlowRides({super.myRides});

  @override
  Future<RideModel> completeRide(String rideId) async {
    await Future<void>.delayed(const Duration(milliseconds: 50));
    return super.completeRide(rideId);
  }
}

class _ControlledArrivalRepository extends FakeSeatRideRepository {
  final arrival = Completer<RideModel>();
  int arrivalCalls = 0;

  @override
  Future<RideModel> arrivePassenger(String rideId, String passengerId) {
    arrivalCalls++;
    return arrival.future;
  }
}
