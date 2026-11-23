// Autor: Cristian Eduardo Fava - RA 25000636

/// Endpoints do módulo de ciclo de vida de caronas, paradas e pagamentos (`/api/v1/rides`).
///
/// Os caminhos com `{passengerId}` agem sobre um passageiro da viagem; o backend confere se quem chama
/// é o motorista ou o próprio passageiro, conforme a ação.
abstract final class RideEndpoints {
  /// Viagens do usuário logado (GET).
  static const String rides = '/rides';

  /// Pedido de vaga no trajeto de um motorista (POST).
  static const String request = '/rides/request';

  /// Cobranças em aberto do usuário logado (com o PIX de cada uma).
  static const String pendingPayments = '/rides/payments/pending';

  /// Uma viagem (GET).
  static String rideById(String rideId) => '/rides/$rideId';

  /// Cancelamento pelo motorista (POST).
  static String cancel(String rideId) => '/rides/$rideId/cancel';

  /// "Iniciar trajeto" (POST).
  static String start(String rideId) => '/rides/$rideId/start';

  /// Encerramento da viagem (POST).
  static String complete(String rideId) => '/rides/$rideId/complete';

  /// Saída do passageiro antes da viagem (POST).
  static String leave(String rideId) => '/rides/$rideId/leave';

  /// Base dos caminhos de um passageiro da viagem.
  static String _passenger(String rideId, String passengerId) =>
      '/rides/$rideId/passengers/$passengerId';

  /// Aceite do pedido (POST, motorista).
  static String accept(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/accept';

  /// Recusa do pedido ou remoção do passageiro (POST, motorista).
  static String reject(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/reject';

  /// Registro de ausência (POST, motorista).
  static String noShow(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/no-show';

  /// Prévia de como a viagem fica com o passageiro aceito (GET, motorista).
  static String preview(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/preview';

  /// "Entrou": embarque confirmado (POST, motorista).
  static String board(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/board';

  /// "Confirmar chegada" (POST, motorista).
  static String arrive(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/arrive';

  /// Cobrança do passageiro (GET, motorista ou o próprio passageiro).
  static String payment(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/payment';

  /// "Já paguei" (POST, só o passageiro).
  static String markPaid(String rideId, String passengerId) =>
      '${payment(rideId, passengerId)}/mark-paid';
}
