// Autor: Cristian Eduardo Fava - RA 25000636

/// Tipos de mensagem do chat de uma carona.
enum ChatMessageType {
  /// Mensagem de texto digitada por um dos dois.
  text('TEXT'),

  /// O motorista solicitou o pagamento do valor combinado.
  paymentRequest('PAYMENT_REQUEST'),

  /// O motorista confirmou que o passageiro já pagou.
  paymentConfirmed('PAYMENT_CONFIRMED'),

  /// Aviso automático da viagem (iniciada, concluída).
  system('SYSTEM');

  final String value;

  const ChatMessageType(this.value);

  /// Mensagens que mudam o estado da viagem ou do pagamento no backend.
  bool get changesRideState => this != text;

  static ChatMessageType fromValue(Object? value) =>
      values.firstWhere((type) => type.value == value, orElse: () => text);
}
