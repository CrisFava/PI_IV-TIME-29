// Autor: Cristian Eduardo Fava - RA 25000636

import 'dart:async';

import 'package:converge/features/chat/data/models/chat_message_model.dart';
import 'package:converge/features/chat/data/models/chat_thread_ref.dart';
import 'package:converge/features/chat/data/repository/chat_exception.dart';
import 'package:converge/features/chat/data/repository/chat_repository.dart';

/// Chat em memória: cada conversa guarda as suas mensagens e avisa quem está ouvindo.
class FakeChatRepository implements ChatRepository {
  final Map<ChatThreadRef, List<ChatMessageModel>> _messages = {};
  final Map<ChatThreadRef, StreamController<List<ChatMessageModel>>>
  _controllers = {};

  /// Quando `true`, todo envio falha como se o Realtime Database estivesse fora do ar.
  bool failOnSend = false;
  int _nextId = 1;

  List<ChatMessageModel> messagesOf(ChatThreadRef thread) =>
      List.unmodifiable(_messages[thread] ?? const []);

  StreamController<List<ChatMessageModel>> _controllerOf(
    ChatThreadRef thread,
  ) => _controllers.putIfAbsent(
    thread,
    () => StreamController<List<ChatMessageModel>>.broadcast(),
  );

  @override
  Stream<List<ChatMessageModel>> watchMessages(ChatThreadRef thread) async* {
    yield messagesOf(thread);
    yield* _controllerOf(thread).stream;
  }

  @override
  Future<void> sendMessage(
    ChatThreadRef thread,
    ChatMessageModel message,
  ) async {
    if (failOnSend) {
      throw const ChatException('Não foi possível enviar a mensagem.');
    }

    final stored = ChatMessageModel(
      id: 'message-${_nextId++}',
      senderId: message.senderId,
      // No Firebase o repositório grava o uid de quem está logado.
      senderUid: 'uid-${message.senderId}',
      senderName: message.senderName,
      type: message.type,
      text: message.text,
      amount: message.amount,
      sentAt: DateTime(2026, 11, 16, 7, 0, _nextId),
    );

    _messages.putIfAbsent(thread, () => []).add(stored);
    _controllerOf(thread).add(messagesOf(thread));
  }

  Future<void> dispose() async {
    for (final controller in _controllers.values) {
      await controller.close();
    }
  }
}
