// Autor: Cristian Eduardo Fava - RA 25000636

/// Horário de uma mensagem no fuso do aparelho, como `07:42`. Vazio enquanto
/// o servidor ainda não gravou o horário.
String formatChatTime(DateTime? sentAt) {
  if (sentAt == null) return '';
  final local = sentAt.toLocal();
  return '${local.hour.toString().padLeft(2, '0')}:'
      '${local.minute.toString().padLeft(2, '0')}';
}
