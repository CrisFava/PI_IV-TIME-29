// Autor: Cristian Eduardo Fava - RA 25000636

/// Falha no chat em tempo real, já com uma mensagem pronta para a interface.
class ChatException implements Exception {
  final String message;

  const ChatException(this.message);

  @override
  String toString() => 'ChatException($message)';
}
