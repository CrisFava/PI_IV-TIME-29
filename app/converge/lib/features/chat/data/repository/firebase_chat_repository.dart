// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:firebase_auth/firebase_auth.dart' as fb;
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_database/firebase_database.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/chat_message_model.dart';
import '../models/chat_thread_ref.dart';
import 'chat_exception.dart';
import 'chat_repository.dart';

/// Sobrescreva em testes com `chatRepositoryProvider.overrideWithValue(...)`.
/// A URL do banco vem do `firebase_options.dart`, gerado pelo FlutterFire CLI.
final chatRepositoryProvider = Provider<ChatRepository>(
  (ref) => FirebaseChatRepository(
    FirebaseDatabase.instance,
    fb.FirebaseAuth.instance,
  ),
);

/// Chat no Firebase Realtime Database.
///
/// Estrutura: `chats/{chatRoomId}/{passengerId}/messages/{pushId}`, onde
/// `chatRoomId` é o da viagem no MongoDB. As regras de acesso ficam em
/// `database.rules.json`.
class FirebaseChatRepository implements ChatRepository {
  /// A conversa de uma carona é curta; o limite só evita baixar um histórico sem fim.
  static const int _messageLimit = 200;

  final FirebaseDatabase _database;
  final fb.FirebaseAuth _auth;

  FirebaseChatRepository(this._database, this._auth);

  DatabaseReference _messages(ChatThreadRef thread) =>
      _database.ref('${thread.path}/messages');

  @override
  Stream<List<ChatMessageModel>> watchMessages(ChatThreadRef thread) =>
      _messages(thread)
          .orderByChild('sentAt')
          .limitToLast(_messageLimit)
          .onValue
          .map((event) => _parse(event.snapshot))
          .handleError(
            (Object error, StackTrace stackTrace) => Error.throwWithStackTrace(
              _toChatException(error, 'Não foi possível carregar a conversa.'),
              stackTrace,
            ),
          );

  @override
  Future<void> sendMessage(
    ChatThreadRef thread,
    ChatMessageModel message,
  ) async {
    try {
      await _messages(thread).push().set({
        ...message.toMap(),
        // As regras conferem estes dois campos: quem envia e o horário do servidor.
        'senderUid': _auth.currentUser?.uid,
        'sentAt': ServerValue.timestamp,
      });
    } on FirebaseException catch (error, stackTrace) {
      Error.throwWithStackTrace(
        _toChatException(error, 'Não foi possível enviar a mensagem.'),
        stackTrace,
      );
    }
  }

  ChatException _toChatException(Object error, String fallback) {
    final denied =
        error is FirebaseException && error.code.contains('permission-denied');
    return ChatException(
      denied ? 'Você não tem acesso a esta conversa.' : fallback,
    );
  }

  List<ChatMessageModel> _parse(DataSnapshot snapshot) => [
    for (final child in snapshot.children)
      if (child.value case final Map<Object?, Object?> value)
        ChatMessageModel.fromMap(child.key ?? '', value),
  ];
}
