// Autor: Cristian Eduardo Fava - RA 25000636

import '../models/ride_model.dart';
import '../models/ride_payment_model.dart';

/// Contrato com a API de viagens (`/rides`).
abstract class RideRepository {
  /// Pede uma vaga no trajeto de um motorista para a data informada.
  Future<RideModel> requestSeat({
    required String driverRouteId,
    required String passengerRouteId,
    required DateTime date,
  });

  /// Viagens em que o usuário participa, como motorista ou passageiro.
  Future<List<RideModel>> listMyRides();

  Future<RideModel> getRide(String rideId);

  // --- Ações do motorista sobre os pedidos de vaga ---
  Future<RideModel> acceptPassenger(String rideId, String passengerId);
  Future<RideModel> rejectPassenger(
    String rideId,
    String passengerId, {
    String? reason,
  });

  // --- Ciclo de vida da viagem ---
  Future<RideModel> startRide(String rideId);
  Future<RideModel> completeRide(String rideId);
  Future<RideModel> cancelRide(String rideId, {String? reason});
  Future<RideModel> leaveRide(String rideId, {String? reason});
  Future<RideModel> markNoShow(String rideId, String passengerId);

  // --- Pagamento ---
  Future<RidePaymentModel> getPayment(String rideId, String passengerId);

  /// "Solicitar pagamento": o motorista cobra o valor combinado no chat.
  Future<RidePaymentModel> requestPayment(
    String rideId,
    String passengerId, {
    required double amount,
  });

  /// "Passageiro já pagou": o motorista confirma o recebimento.
  Future<RidePaymentModel> confirmPayment(String rideId, String passengerId);
}
