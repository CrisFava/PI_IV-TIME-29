// Autor: Cristian Eduardo Fava - RA 25000636

/// Horário de uma mensagem no fuso do aparelho, como `07:42`. Vazio enquanto
/// o servidor ainda não gravou o horário.
String formatChatTime(DateTime? sentAt) {
  if (sentAt == null) return '';
  final local = sentAt.toLocal();
  return '${local.hour.toString().padLeft(2, '0')}:'
      '${local.minute.toString().padLeft(2, '0')}';
}

/// O dia de uma mensagem na etiqueta que separa os dias da conversa: `Hoje`,
/// `Ontem` ou a data completa (`17/11/2026`). [now] é o momento de agora no aparelho.
String formatChatDay(DateTime time, {required DateTime now}) {
  final local = time.toLocal();
  final today = DateTime(now.year, now.month, now.day);
  final day = DateTime(local.year, local.month, local.day);

  return switch (today.difference(day).inDays) {
    <= 0 => 'Hoje',
    1 => 'Ontem',
    _ =>
      '${local.day.toString().padLeft(2, '0')}/'
          '${local.month.toString().padLeft(2, '0')}/${local.year}',
  };
}

/// Quando foi a última mensagem, para a lista de conversas: a hora (`07:42`) se foi
/// hoje, `Ontem`, ou o dia e o mês (`17/11`). [now] é o momento de agora no aparelho.
String formatConversationTime(DateTime time, {required DateTime now}) {
  final local = time.toLocal();
  final today = DateTime(now.year, now.month, now.day);
  final day = DateTime(local.year, local.month, local.day);

  return switch (today.difference(day).inDays) {
    <= 0 => formatChatTime(local),
    1 => 'Ontem',
    _ =>
      '${local.day.toString().padLeft(2, '0')}/'
          '${local.month.toString().padLeft(2, '0')}',
  };
}
