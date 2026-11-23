// Autor: Cristian Eduardo Fava - RA 25000636

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/models/user_model.dart';
import '../../auth/state/auth_action_controller.dart';
import '../../auth/state/auth_controller.dart';
import '../../auth/state/session/auth_session.dart';
import '../../ride/data/models/ride_model.dart';
import '../../ride/data/models/ride_status.dart';
import '../../ride/state/ride_providers.dart';
import '../data/models/chat_conversation.dart';
import '../data/models/chat_message_model.dart';
import '../data/models/chat_thread_ref.dart';
import '../data/repository/chat_exception.dart';
import '../data/repository/firebase_chat_repository.dart';

/// O usuário logado, ou `null` fora de uma sessão completa.
final chatViewerProvider = Provider.autoDispose<UserModel?>((ref) {
  final session = ref.watch(authControllerProvider).value;
  return session is Authenticated ? session.user : null;
});

/// Quanto tempo a conversa fica disponível depois do fim da viagem.
const chatLifetime = Duration(days: 7);

/// A conversa continua aberta enquanto a viagem estiver marcada ou em andamento,
/// e até [chatLifetime] depois do fim dela (a conclusão ou, sem ela, o horário de saída).
bool isChatOpen(RideModel ride, DateTime now) {
  switch (ride.status) {
    case RideStatus.inProgress ||
        RideStatus.awaitingConfirmation ||
        RideStatus.confirmed:
      return true;
    default:
      final end = ride.completedAt ?? ride.departureDateTime;
      return end != null && now.isBefore(end.add(chatLifetime));
  }
}

/// Conversas liberadas para o usuário logado: **uma por sala** (o grupo de cada viagem),
/// ordenadas pela relevância: as em andamento, depois as próximas (da mais perto) e por fim as
/// que já terminaram (da mais recente). Viagens canceladas não têm conversa.
/// A conversa expira uma semana depois do fim da viagem ([isChatOpen]); com a lista aberta, um
/// timer recarrega o provider quando a primeira delas expira.
/// Recarregue com `ref.invalidate(myRidesProvider)`.
final conversationsProvider =
    FutureProvider.autoDispose<List<ChatConversation>>((ref) async {
      final viewer = ref.watch(chatViewerProvider);
      if (viewer == null) return const [];

      final rides = await ref.watch(myRidesProvider.future);
      final all = [
        for (final ride in rides)
          if (ride.status != RideStatus.cancelled)
            ...ChatConversation.ofRide(ride, viewer.id),
      ]..sort(_byRelevance);

      final byPair = <String, ChatConversation>{};
      for (final conversation in all) {
        byPair.putIfAbsent(conversation.thread.path, () => conversation);
      }
      final now = DateTime.now();
      final open = [
        for (final conversation in byPair.values)
          if (isChatOpen(conversation.ride, now)) conversation,
      ];

      // Com a tela aberta, a conversa que expira primeiro some na hora certa
      DateTime? nextExpiry;
      for (final conversation in open) {
        final expiry = _expiryOf(conversation.ride);
        if (expiry != null &&
            (nextExpiry == null || expiry.isBefore(nextExpiry))) {
          nextExpiry = expiry;
        }
      }
      if (nextExpiry != null) {
        final timer = Timer(
          nextExpiry.difference(now) + const Duration(seconds: 1),
          ref.invalidateSelf,
        );
        ref.onDispose(timer.cancel);
      }
      return open;
    });

/// Quando a conversa expira; nulo enquanto a viagem não terminou.
DateTime? _expiryOf(RideModel ride) => switch (ride.status) {
  RideStatus.inProgress ||
  RideStatus.awaitingConfirmation ||
  RideStatus.confirmed => null,
  _ => (ride.completedAt ?? ride.departureDateTime)?.add(chatLifetime),
};

/// Ordem das conversas: em andamento primeiro; depois as próximas (da mais perto); depois as que já
/// terminaram (da mais recente).
int _byRelevance(ChatConversation a, ChatConversation b) {
  int group(ChatConversation c) => switch (c.ride.status) {
    RideStatus.inProgress => 0,
    RideStatus.awaitingConfirmation || RideStatus.confirmed => 1,
    _ => 2,
  };
  final byGroup = group(a).compareTo(group(b));
  if (byGroup != 0) return byGroup;

  final aDate = a.ride.departureDateTime ?? DateTime(0);
  final bDate = b.ride.departureDateTime ?? DateTime(0);
  return group(a) == 1 ? aDate.compareTo(bDate) : bDate.compareTo(aDate);
}

/// Mensagens de uma conversa, em tempo real.
final chatMessagesProvider = StreamProvider.autoDispose
    .family<List<ChatMessageModel>, ChatThreadRef>(
      (ref, thread) => ref.watch(chatRepositoryProvider).watchMessages(thread),
    );

/// Mensagem amigável para qualquer erro das telas de chat.
///
/// [ChatException] já traz a mensagem; os demais erros (API, autenticação) usam [authErrorMessage].
String chatErrorMessage(Object error) => switch (error) {
  ChatException(:final message) => message,
  _ => authErrorMessage(error),
};
