// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/ride/data/models/payment_status.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/models/ride_payment_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository.dart';

import 'fake_ride.dart';

/// Repositório de viagens em memória: guarda uma viagem e o pagamento do passageiro.
class FakeRideRepository implements RideRepository {
  RideModel ride;
  RidePaymentModel payment;

  /// Viagem devolvida ao concluir. Sem ela, devolve a viagem padrão concluída.
  RideModel? completedRide;

  /// Quando preenchido, a próxima ação do motorista falha com esta mensagem.
  String? failNextActionWith;

  final List<String> calls = [];

  FakeRideRepository({RideModel? ride, RidePaymentModel? payment})
    : ride = ride ?? fakeRide(),
      payment = payment ?? fakePayment();

  static RidePaymentModel fakePayment({
    double? amount,
    PaymentStatus status = PaymentStatus.pending,
  }) => RidePaymentModel(
    rideId: fakeRideId,
    passengerId: fakePassengerId,
    driverId: fakeDriverId,
    receiverName: 'Lucas Silveira',
    amount: amount,
    status: status,
    confirmedByDriver: status == PaymentStatus.paid,
    pixCopyPaste: amount != null && status == PaymentStatus.pending
        ? '000201-PIX-FALSO'
        : null,
    requestedAt: amount == null ? null : DateTime(2026, 11, 16, 7),
    paidAt: status == PaymentStatus.paid ? DateTime(2026, 11, 16, 9) : null,
  );

  void _failIfRequested() {
    final message = failNextActionWith;
    if (message == null) return;

    failNextActionWith = null;
    throw ApiException(
      code: ApiErrorCode.badRequest,
      message: message,
      status: 400,
    );
  }

  @override
  Future<List<RideModel>> listMyRides() async {
    calls.add('listMyRides');
    return [ride];
  }

  @override
  Future<RideModel> getRide(String rideId) async {
    calls.add('getRide');
    return ride;
  }

  @override
  Future<RidePaymentModel> getPayment(String rideId, String passengerId) async {
    calls.add('getPayment');
    return payment;
  }

  @override
  Future<RidePaymentModel> requestPayment(
    String rideId,
    String passengerId, {
    required double amount,
  }) async {
    calls.add('requestPayment:$amount');
    _failIfRequested();
    return payment = fakePayment(amount: amount);
  }

  @override
  Future<RidePaymentModel> confirmPayment(
    String rideId,
    String passengerId,
  ) async {
    calls.add('confirmPayment');
    _failIfRequested();
    return payment = fakePayment(
      amount: payment.amount,
      status: PaymentStatus.paid,
    );
  }

  @override
  Future<RideModel> startRide(String rideId) async {
    calls.add('startRide');
    _failIfRequested();
    return ride = fakeRide(status: 'IN_PROGRESS');
  }

  @override
  Future<RideModel> completeRide(String rideId) async {
    calls.add('completeRide');
    _failIfRequested();
    return ride =
        completedRide ??
        fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED');
  }

  @override
  Future<RideModel> requestSeat({
    required String driverRouteId,
    required String passengerRouteId,
    required DateTime date,
  }) => throw UnimplementedError();

  @override
  Future<RideModel> acceptPassenger(String rideId, String passengerId) =>
      throw UnimplementedError();

  @override
  Future<RideModel> rejectPassenger(
    String rideId,
    String passengerId, {
    String? reason,
  }) => throw UnimplementedError();

  @override
  Future<RideModel> cancelRide(String rideId, {String? reason}) =>
      throw UnimplementedError();

  @override
  Future<RideModel> leaveRide(String rideId, {String? reason}) =>
      throw UnimplementedError();

  @override
  Future<RideModel> markNoShow(String rideId, String passengerId) =>
      throw UnimplementedError();
}
