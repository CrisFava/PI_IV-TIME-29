// Autor: Cristian Eduardo Fava - RA 25000636

import 'dart:async';

import 'package:converge/features/chat/data/models/chat_message_model.dart';
import 'package:converge/features/chat/data/models/chat_thread_ref.dart';
import 'package:converge/features/chat/data/repository/chat_exception.dart';
import 'package:converge/features/chat/data/repository/chat_repository.dart';

/// Chat em memória: cada conversa guarda as suas mensagens e avisa quem está ouvindo.
class FakeChatRepository implements ChatRepository {
  /// Mensagens por caminho, como no Realtime Database: o grupo da viagem é um lugar só.
  final Map<String, List<ChatMessageModel>> _messages = {};

  /// Um stream por conversa, para avisar quem está ouvindo a cada envio.
  final Map<String, StreamController<List<ChatMessageModel>>> _controllers = {};

  /// Quando `true`, todo envio falha como se o Realtime Database estivesse fora do ar.
  bool failOnSend = false;

  /// Próximo número usado no id e nos segundos do horário das mensagens.
  int _nextId = 1;

  /// Mensagens gravadas na conversa, para o teste conferir.
  List<ChatMessageModel> messagesOf(ChatThreadRef thread) =>
      List.unmodifiable(_messages[thread.path] ?? const []);

  /// Stream da conversa, criado na primeira vez que alguém ouve ou envia.
  StreamController<List<ChatMessageModel>> _controllerOf(
    ChatThreadRef thread,
  ) => _controllers.putIfAbsent(
    thread.path,
    () => StreamController<List<ChatMessageModel>>.broadcast(),
  );

  /// Emite as mensagens atuais e depois a lista a cada envio.
  @override
  Stream<List<ChatMessageModel>> watchMessages(ChatThreadRef thread) async* {
    yield messagesOf(thread);
    yield* _controllerOf(thread).stream;
  }

  /// Grava a mensagem com id, `senderUid` e horário gerados aqui (no app real, pelo Firebase) e
  /// avisa os ouvintes. Com [failOnSend], lança [ChatException].
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

    _messages.putIfAbsent(thread.path, () => []).add(stored);
    _controllerOf(thread).add(messagesOf(thread));
  }

  /// Fecha os streams; chame no `tearDown` dos testes.
  Future<void> dispose() async {
    for (final controller in _controllers.values) {
      await controller.close();
    }
  }
}
