// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/ride/data/models/payment_status.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/models/ride_payment_model.dart';
import 'package:converge/features/ride/data/models/ride_preview_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository.dart';

import 'fake_ride.dart';

/// Repositório de viagens em memória: guarda uma viagem e o pagamento do passageiro.
///
/// As ações do motorista trocam [ride] pela viagem no novo status, e toda chamada fica registrada em
/// [calls], para o teste conferir o que a tela ou o controller pediram à API.
class FakeRideRepository implements RideRepository {
  /// A viagem "gravada na API".
  RideModel ride;

  /// O pagamento do passageiro "gravado na API".
  RidePaymentModel payment;

  /// Viagem devolvida ao concluir. Sem ela, devolve a viagem padrão concluída.
  RideModel? completedRide;

  /// Quando preenchido, a próxima ação do motorista falha com esta mensagem.
  String? failNextActionWith;

  /// Nome de cada método chamado, na ordem (com o id quando ele importa, ex.: `arrivePassenger:p1`).
  final List<String> calls = [];

  FakeRideRepository({RideModel? ride, RidePaymentModel? payment})
    : ride = ride ?? fakeRide(),
      payment = payment ?? fakePayment();

  /// Pagamento do passageiro principal. Com [amount], é uma cobrança emitida; pendente, ela traz um
  /// código PIX falso; paga, traz a data do pagamento.
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
    pixCopyPaste: amount != null && status == PaymentStatus.pending
        ? '000201-PIX-FALSO'
        : null,
    requestedAt: amount == null ? null : DateTime(2026, 11, 16, 7),
    paidAt: status == PaymentStatus.paid ? DateTime(2026, 11, 16, 9) : null,
  );

  /// Lança a falha programada em [failNextActionWith] uma única vez, como um BAD_REQUEST da API.
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

  /// Lista só com a viagem guardada.
  @override
  Future<List<RideModel>> listMyRides() async {
    calls.add('listMyRides');
    return [ride];
  }

  /// Devolve a viagem guardada, qualquer que seja o id.
  @override
  Future<RideModel> getRide(String rideId) async {
    calls.add('getRide');
    return ride;
  }

  /// Devolve o pagamento guardado.
  @override
  Future<RidePaymentModel> getPayment(String rideId, String passengerId) async {
    calls.add('getPayment');
    return payment;
  }

  /// Cobranças em aberto devolvidas por [listPendingPayments].
  List<RidePaymentModel> pendingPayments = [];

  /// Devolve [pendingPayments].
  @override
  Future<List<RidePaymentModel>> listPendingPayments() async {
    calls.add('listPendingPayments');
    return pendingPayments;
  }

  /// Tira a cobrança da viagem das pendentes e marca o pagamento como pago.
  @override
  Future<RidePaymentModel> markPaymentAsPaid(
    String rideId,
    String passengerId,
  ) async {
    calls.add('markPaymentAsPaid:$rideId');
    _failIfRequested();
    pendingPayments = [
      for (final pending in pendingPayments)
        if (pending.rideId != rideId) pending,
    ];
    return payment = fakePayment(
      amount: payment.amount,
      status: PaymentStatus.paid,
    );
  }

  /// Não usado pelos testes.
  @override
  Future<RidePreviewModel> previewPassenger(String rideId, String passengerId) =>
      throw UnimplementedError();

  /// "Entrou": a viagem segue em andamento.
  @override
  Future<RideModel> boardPassenger(String rideId, String passengerId) async {
    calls.add('boardPassenger:$passengerId');
    _failIfRequested();
    return ride = fakeRide(status: 'IN_PROGRESS');
  }

  /// "Confirmar chegada": o passageiro passa a COMPLETED com a viagem ainda em andamento.
  @override
  Future<RideModel> arrivePassenger(String rideId, String passengerId) async {
    calls.add('arrivePassenger:$passengerId');
    _failIfRequested();
    return ride = fakeRide(status: 'IN_PROGRESS', passengerStatus: 'COMPLETED');
  }

  /// A viagem passa a IN_PROGRESS.
  @override
  Future<RideModel> startRide(String rideId) async {
    calls.add('startRide');
    _failIfRequested();
    return ride = fakeRide(status: 'IN_PROGRESS');
  }

  /// A viagem passa a COMPLETED (ou vira [completedRide], se o teste definiu uma).
  @override
  Future<RideModel> completeRide(String rideId) async {
    calls.add('completeRide');
    _failIfRequested();
    return ride =
        completedRide ??
        fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED');
  }

  /// Não usado pelos testes.
  @override
  Future<RideModel> requestSeat({
    required String driverRouteId,
    required String passengerRouteId,
    required DateTime date,
  }) => throw UnimplementedError();

  /// Não usado pelos testes.
  @override
  Future<RideModel> acceptPassenger(String rideId, String passengerId) =>
      throw UnimplementedError();

  /// Não usado pelos testes.
  @override
  Future<RideModel> rejectPassenger(
    String rideId,
    String passengerId, {
    String? reason,
  }) => throw UnimplementedError();

  /// Não usado pelos testes.
  @override
  Future<RideModel> cancelRide(String rideId, {String? reason}) =>
      throw UnimplementedError();

  /// Não usado pelos testes.
  @override
  Future<RideModel> leaveRide(String rideId, {String? reason}) =>
      throw UnimplementedError();

  /// Não usado pelos testes.
  @override
  Future<RideModel> markNoShow(String rideId, String passengerId) =>
      throw UnimplementedError();
}
