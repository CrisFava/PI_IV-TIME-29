// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/ride_model.dart';
import '../models/ride_payment_model.dart';
import '../models/ride_preview_model.dart';
import 'ride_endpoints.dart';
import 'ride_repository.dart';

/// Provedor global da instância de [RideRepository].
final rideRepositoryProvider = Provider<RideRepository>(
  (ref) => RideRepositoryImpl(ref.watch(dioProvider)),
);

/// Implementação do repositório de caronas, solicitações, paradas e fluxo de pagamento.
///
/// As ações sobre a viagem (aceitar, iniciar, concluir etc.) devolvem a viagem já atualizada, para a
/// tela não precisar buscá-la de novo.
class RideRepositoryImpl implements RideRepository {
  /// Cliente HTTP com os interceptors de autenticação, idempotência e erro.
  final Dio _dio;

  RideRepositoryImpl(this._dio);

  /// `POST /rides/request` com a data só com o dia (`yyyy-MM-dd`).
  @override
  Future<RideModel> requestSeat({
    required String driverRouteId,
    required String passengerRouteId,
    required DateTime date,
  }) => _postRide(
    RideEndpoints.request,
    data: {
      'driverRouteId': driverRouteId,
      'passengerRouteId': passengerRouteId,
      'date': _isoDate(date),
    },
  );

  /// `GET /rides`.
  @override
  Future<List<RideModel>> listMyRides() async {
    try {
      final response = await _dio.get<dynamic>(RideEndpoints.rides);
      return unwrapApiListResponse<RideModel>(
        response.data,
        RideModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  /// `GET /rides/{rideId}`.
  @override
  Future<RideModel> getRide(String rideId) async {
    try {
      final response = await _dio.get<dynamic>(RideEndpoints.rideById(rideId));
      return unwrapApiResponse<RideModel>(
        response.data,
        RideModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  /// `POST .../accept`.
  @override
  Future<RideModel> acceptPassenger(String rideId, String passengerId) =>
      _postRide(RideEndpoints.accept(rideId, passengerId));

  /// `POST .../reject`; o motivo só vai no corpo quando informado.
  @override
  Future<RideModel> rejectPassenger(
    String rideId,
    String passengerId, {
    String? reason,
  }) => _postRide(
    RideEndpoints.reject(rideId, passengerId),
    data: {'reason': ?reason},
  );

  /// `POST /rides/{rideId}/start`.
  @override
  Future<RideModel> startRide(String rideId) =>
      _postRide(RideEndpoints.start(rideId));

  /// `POST /rides/{rideId}/complete`.
  @override
  Future<RideModel> completeRide(String rideId) =>
      _postRide(RideEndpoints.complete(rideId));

  /// `POST /rides/{rideId}/cancel`; o motivo só vai no corpo quando informado.
  @override
  Future<RideModel> cancelRide(String rideId, {String? reason}) =>
      _postRide(RideEndpoints.cancel(rideId), data: {'reason': ?reason});

  /// `POST /rides/{rideId}/leave`; o motivo só vai no corpo quando informado.
  @override
  Future<RideModel> leaveRide(String rideId, {String? reason}) =>
      _postRide(RideEndpoints.leave(rideId), data: {'reason': ?reason});

  /// `POST .../no-show`.
  @override
  Future<RideModel> markNoShow(String rideId, String passengerId) =>
      _postRide(RideEndpoints.noShow(rideId, passengerId));

  /// `GET .../preview`.
  @override
  Future<RidePreviewModel> previewPassenger(
    String rideId,
    String passengerId,
  ) async {
    try {
      final response = await _dio.get<dynamic>(
        RideEndpoints.preview(rideId, passengerId),
      );
      return unwrapApiResponse<RidePreviewModel>(
        response.data,
        RidePreviewModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  /// `POST .../board`.
  @override
  Future<RideModel> boardPassenger(String rideId, String passengerId) =>
      _postRide(RideEndpoints.board(rideId, passengerId));

  /// `POST .../arrive`.
  @override
  Future<RideModel> arrivePassenger(String rideId, String passengerId) =>
      _postRide(RideEndpoints.arrive(rideId, passengerId));

  /// `GET .../payment`.
  @override
  Future<RidePaymentModel> getPayment(String rideId, String passengerId) async {
    try {
      final response = await _dio.get<dynamic>(
        RideEndpoints.payment(rideId, passengerId),
      );
      return _unwrapPayment(response);
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  /// `GET /rides/payments/pending`.
  @override
  Future<List<RidePaymentModel>> listPendingPayments() async {
    try {
      final response = await _dio.get<dynamic>(RideEndpoints.pendingPayments);
      return unwrapApiListResponse<RidePaymentModel>(
        response.data,
        RidePaymentModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  /// `POST .../payment/mark-paid`.
  @override
  Future<RidePaymentModel> markPaymentAsPaid(
    String rideId,
    String passengerId,
  ) async {
    try {
      final response = await _dio.post<dynamic>(
        RideEndpoints.markPaid(rideId, passengerId),
      );
      return _unwrapPayment(response);
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  /// POST que devolve a viagem atualizada no envelope da API.
  Future<RideModel> _postRide(String path, {Map<String, dynamic>? data}) async {
    try {
      final response = await _dio.post<dynamic>(path, data: data);
      return unwrapApiResponse<RideModel>(
        response.data,
        RideModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  /// Lê o pagamento do envelope da API.
  RidePaymentModel _unwrapPayment(Response<dynamic> response) =>
      unwrapApiResponse<RidePaymentModel>(
        response.data,
        RidePaymentModel.fromJson,
        status: response.statusCode ?? 200,
      );

  /// Data como `yyyy-MM-dd`, o formato do `LocalDate` do backend (sem hora nem fuso).
  static String _isoDate(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';
}
