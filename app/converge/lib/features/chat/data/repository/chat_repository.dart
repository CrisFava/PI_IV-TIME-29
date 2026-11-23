// Autor: Cristian Eduardo Fava - RA 25000636

import '../models/chat_message_model.dart';
import '../models/chat_thread_ref.dart';

/// Contrato do chat em tempo real. A implementação oficial usa o Firebase
/// Realtime Database (RT19); nos testes entra uma versão em memória.
abstract class ChatRepository {
  /// Mensagens da conversa em ordem cronológica, reemitidas a cada mudança.
  Stream<List<ChatMessageModel>> watchMessages(ChatThreadRef thread);

  /// Grava uma mensagem nova na conversa. Lança `ChatException` se não for possível.
  Future<void> sendMessage(ChatThreadRef thread, ChatMessageModel message);
}
