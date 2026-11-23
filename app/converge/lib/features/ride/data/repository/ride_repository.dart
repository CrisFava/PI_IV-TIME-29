// Autor: Cristian Eduardo Fava - RA 25000636

import '../models/ride_model.dart';
import '../models/ride_payment_model.dart';
import '../models/ride_preview_model.dart';

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

  /// Uma viagem, no recorte que o usuário logado pode ver.
  Future<RideModel> getRide(String rideId);

  // --- Ações do motorista sobre os pedidos de vaga ---

  /// Aceita o pedido de vaga (RF19); a API grava o teto do passageiro e libera o chat.
  Future<RideModel> acceptPassenger(String rideId, String passengerId);

  /// Recusa um pedido ou remove um passageiro confirmado, com motivo opcional.
  Future<RideModel> rejectPassenger(
    String rideId,
    String passengerId, {
    String? reason,
  });

  // --- Ciclo de vida da viagem ---

  /// "Iniciar trajeto" (só vale de 5 min antes a 5 min depois da saída).
  Future<RideModel> startRide(String rideId);

  /// Encerra a viagem; quem ainda estava a bordo é cobrado com o valor final do rateio.
  Future<RideModel> completeRide(String rideId);

  /// O motorista cancela a viagem, com motivo opcional (RF28).
  Future<RideModel> cancelRide(String rideId, {String? reason});

  /// O passageiro sai da viagem antes de ela começar, com motivo opcional.
  Future<RideModel> leaveRide(String rideId, {String? reason});

  /// O motorista registra a ausência do passageiro (RF23); ele não é cobrado.
  Future<RideModel> markNoShow(String rideId, String passengerId);

  /// Como a viagem ficaria aceitando o pedido do passageiro (nada é gravado).
  Future<RidePreviewModel> previewPassenger(String rideId, String passengerId);

  /// "Entrou": o motorista confirma que o passageiro embarcou.
  Future<RideModel> boardPassenger(String rideId, String passengerId);

  /// "Confirmar chegada": o passageiro desceu e já recebe a cobrança.
  Future<RideModel> arrivePassenger(String rideId, String passengerId);

  // --- Pagamento ---

  /// Cobrança de um passageiro, com o PIX se estiver em aberto.
  Future<RidePaymentModel> getPayment(String rideId, String passengerId);

  /// Cobranças em aberto do usuário logado, com o PIX de cada uma.
  Future<List<RidePaymentModel>> listPendingPayments();

  /// "Já paguei": o passageiro informa o pagamento da própria cobrança.
  Future<RidePaymentModel> markPaymentAsPaid(String rideId, String passengerId);
}
