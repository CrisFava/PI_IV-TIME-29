// Autor: Cristian Eduardo Fava - RA 25000636

/// Tipos de mensagem do chat de uma carona.
///
/// Hoje o app só envia [text] e [system]: a cobrança sai automática no fim da viagem e aparece na tela
/// de pagamento de cada passageiro, não no grupo. Os tipos de pagamento continuam aqui para as
/// mensagens antigas ainda serem exibidas.
enum ChatMessageType {
  /// Mensagem de texto digitada por um participante.
  text('TEXT'),

  /// Cobrança de um valor combinado no chat (fluxo antigo, quando o motorista cobrava pelo chat).
  paymentRequest('PAYMENT_REQUEST'),

  /// Aviso de pagamento confirmado (fluxo antigo).
  paymentConfirmed('PAYMENT_CONFIRMED'),

  /// Aviso automático da viagem (iniciada, concluída).
  system('SYSTEM');

  /// Valor gravado no campo `type` do Realtime Database.
  final String value;

  const ChatMessageType(this.value);

  /// Mensagens que mudam o estado da viagem ou do pagamento no backend. Quando uma delas chega, a tela
  /// recarrega a viagem e o pagamento pela API.
  bool get changesRideState => this != text;

  /// Lê o valor gravado; desconhecido ou ausente vira [text].
  static ChatMessageType fromValue(Object? value) =>
      values.firstWhere((type) => type.value == value, orElse: () => text);
}
