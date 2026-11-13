// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/models/ride_payment_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository.dart';

import '../../ride/fakes/fake_ride.dart';

/// Repositório de viagens em memória para o pedido de vaga e a resposta do motorista.
/// [passengerStatus] é o status do passageiro devolvido pela próxima consulta da viagem.
class FakeSeatRideRepository implements RideRepository {
  String passengerStatus = 'REQUESTED';
  List<RideModel> myRides;

  /// Quando preenchido, a próxima ação falha com esta mensagem.
  String? failNextWith;

  final List<String> calls = [];

  FakeSeatRideRepository({this.myRides = const []});

  RideModel get _ride => fakeRide(
    status: 'AWAITING_CONFIRMATION',
    passengerStatus: passengerStatus,
  );

  void _maybeFail() {
    final message = failNextWith;
    if (message == null) return;
    failNextWith = null;
    throw ApiException(
      code: ApiErrorCode.badRequest,
      message: message,
      status: 400,
    );
  }

  @override
  Future<RideModel> requestSeat({
    required String driverRouteId,
    required String passengerRouteId,
    required DateTime date,
  }) async {
    calls.add(
      'requestSeat:$driverRouteId:$passengerRouteId:${date.toIso8601String().substring(0, 10)}',
    );
    _maybeFail();
    return _ride;
  }

  @override
  Future<RideModel> getRide(String rideId) async {
    calls.add('getRide:$rideId');
    return _ride;
  }

  @override
  Future<RideModel> leaveRide(String rideId, {String? reason}) async {
    calls.add('leaveRide:$rideId');
    _maybeFail();
    passengerStatus = 'CANCELLED';
    return _ride;
  }

  @override
  Future<List<RideModel>> listMyRides() async {
    calls.add('listMyRides');
    return myRides;
  }

  @override
  Future<RideModel> acceptPassenger(String rideId, String passengerId) async {
    calls.add('accept:$rideId:$passengerId');
    _maybeFail();
    passengerStatus = 'CONFIRMED';
    return _ride;
  }

  @override
  Future<RideModel> rejectPassenger(
    String rideId,
    String passengerId, {
    String? reason,
  }) async {
    calls.add('reject:$rideId:$passengerId');
    _maybeFail();
    passengerStatus = 'REJECTED';
    return _ride;
  }

  @override
  Future<RideModel> startRide(String rideId) async {
    calls.add('start:$rideId');
    _maybeFail();
    return fakeRide(status: 'IN_PROGRESS');
  }

  @override
  Future<RideModel> completeRide(String rideId) async {
    calls.add('complete:$rideId');
    _maybeFail();
    return fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED');
  }

  @override
  Future<RideModel> cancelRide(String rideId, {String? reason}) =>
      throw UnimplementedError();

  @override
  Future<RideModel> markNoShow(String rideId, String passengerId) =>
      throw UnimplementedError();

  @override
  Future<RidePaymentModel> getPayment(String rideId, String passengerId) =>
      throw UnimplementedError();

  @override
  Future<RidePaymentModel> requestPayment(
    String rideId,
    String passengerId, {
    required double amount,
  }) => throw UnimplementedError();

  @override
  Future<RidePaymentModel> confirmPayment(String rideId, String passengerId) =>
      throw UnimplementedError();
}
