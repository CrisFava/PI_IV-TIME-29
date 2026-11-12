// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/models/user_model.dart';
import '../../auth/state/auth_action_controller.dart';
import '../../auth/state/auth_controller.dart';
import '../../auth/state/session/auth_session.dart';
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

/// Conversas liberadas para o usuário logado: uma por carona aceita.
/// Recarregue com `ref.invalidate(myRidesProvider)`.
final conversationsProvider =
    FutureProvider.autoDispose<List<ChatConversation>>((ref) async {
      final viewer = ref.watch(chatViewerProvider);
      if (viewer == null) return const [];

      final rides = await ref.watch(myRidesProvider.future);
      return [
        for (final ride in rides)
          if (ride.status != RideStatus.cancelled)
            ...ChatConversation.ofRide(ride, viewer.id),
      ];
    });

/// Mensagens de uma conversa, em tempo real.
final chatMessagesProvider = StreamProvider.autoDispose
    .family<List<ChatMessageModel>, ChatThreadRef>(
      (ref, thread) => ref.watch(chatRepositoryProvider).watchMessages(thread),
    );

/// Mensagem amigável para qualquer erro das telas de chat.
String chatErrorMessage(Object error) => switch (error) {
  ChatException(:final message) => message,
  _ => authErrorMessage(error),
};
