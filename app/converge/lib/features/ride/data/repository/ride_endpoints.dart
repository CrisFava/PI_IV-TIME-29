// Autor: Cristian Eduardo Fava - RA 25000636

abstract final class RideEndpoints {
  static const String rides = '/rides';
  static const String request = '/rides/request';

  /// Cobranças em aberto do usuário logado (com o PIX de cada uma).
  static const String pendingPayments = '/rides/payments/pending';

  static String rideById(String rideId) => '/rides/$rideId';
  static String cancel(String rideId) => '/rides/$rideId/cancel';
  static String start(String rideId) => '/rides/$rideId/start';
  static String complete(String rideId) => '/rides/$rideId/complete';
  static String leave(String rideId) => '/rides/$rideId/leave';

  static String _passenger(String rideId, String passengerId) =>
      '/rides/$rideId/passengers/$passengerId';

  static String accept(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/accept';
  static String reject(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/reject';
  static String noShow(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/no-show';
  static String arrive(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/arrive';

  static String payment(String rideId, String passengerId) =>
      '${_passenger(rideId, passengerId)}/payment';
  static String markPaid(String rideId, String passengerId) =>
      '${payment(rideId, passengerId)}/mark-paid';
}
