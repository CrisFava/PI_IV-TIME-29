// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

final chatReadStoreProvider = Provider<ChatReadStore>(
  (ref) => const ChatReadStore(),
);

/// Até quando o usuário já leu cada conversa, guardado no aparelho. Serve para o
/// número de mensagens não vistas na navbar (o Firebase do chat só guarda mensagens).
class ChatReadStore {
  static const String _prefix = 'chat_last_read::';

  const ChatReadStore();

  /// Leituras do usuário: caminho da conversa → até quando foi lida.
  /// Se o armazenamento do aparelho falhar, nada conta como lido (sem quebrar a tela).
  Future<Map<String, DateTime>> load(String userId) async {
    final SharedPreferences prefs;
    try {
      prefs = await SharedPreferences.getInstance();
    } catch (_) {
      return const {};
    }
    final keyPrefix = '$_prefix$userId::';
    return {
      for (final key in prefs.getKeys())
        if (key.startsWith(keyPrefix))
          key.substring(keyPrefix.length): DateTime.fromMillisecondsSinceEpoch(
            prefs.getInt(key) ?? 0,
          ),
    };
  }

  Future<void> save(String userId, String threadPath, DateTime readAt) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt(
        '$_prefix$userId::$threadPath',
        readAt.millisecondsSinceEpoch,
      );
    } catch (_) {
      // Sem gravar no aparelho, a leitura vale só até fechar o app
    }
  }
}
