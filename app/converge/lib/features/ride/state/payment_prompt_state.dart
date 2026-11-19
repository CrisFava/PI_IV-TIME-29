// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

/// Estado do pagamento que o app mostra ao passageiro.
@immutable
class PaymentPromptState {
  /// Viagens cuja cobrança o passageiro fechou com "Depois" nesta sessão: não abrem sozinhas
  /// de novo até ele tentar pedir ou oferecer uma carona.
  final Set<String> dismissedRideIds;

  /// Viagem com o "Já paguei" sendo enviado (desabilita o botão).
  final String? markingRideId;

  const PaymentPromptState({
    this.dismissedRideIds = const {},
    this.markingRideId,
  });

  bool get isMarking => markingRideId != null;
}
