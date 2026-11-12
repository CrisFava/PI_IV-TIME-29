// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/ride_model.dart';
import '../models/ride_payment_model.dart';
import 'ride_endpoints.dart';
import 'ride_repository.dart';

final rideRepositoryProvider = Provider<RideRepository>(
  (ref) => RideRepositoryImpl(ref.watch(dioProvider)),
);

class RideRepositoryImpl implements RideRepository {
  final Dio _dio;

  RideRepositoryImpl(this._dio);

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

  @override
  Future<RideModel> acceptPassenger(String rideId, String passengerId) =>
      _postRide(RideEndpoints.accept(rideId, passengerId));

  @override
  Future<RideModel> rejectPassenger(
    String rideId,
    String passengerId, {
    String? reason,
  }) => _postRide(
    RideEndpoints.reject(rideId, passengerId),
    data: {'reason': ?reason},
  );

  @override
  Future<RideModel> startRide(String rideId) =>
      _postRide(RideEndpoints.start(rideId));

  @override
  Future<RideModel> completeRide(String rideId) =>
      _postRide(RideEndpoints.complete(rideId));

  @override
  Future<RideModel> cancelRide(String rideId, {String? reason}) =>
      _postRide(RideEndpoints.cancel(rideId), data: {'reason': ?reason});

  @override
  Future<RideModel> leaveRide(String rideId, {String? reason}) =>
      _postRide(RideEndpoints.leave(rideId), data: {'reason': ?reason});

  @override
  Future<RideModel> markNoShow(String rideId, String passengerId) =>
      _postRide(RideEndpoints.noShow(rideId, passengerId));

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

  @override
  Future<RidePaymentModel> requestPayment(
    String rideId,
    String passengerId, {
    required double amount,
  }) async {
    try {
      final response = await _dio.post<dynamic>(
        RideEndpoints.requestPayment(rideId, passengerId),
        // Como texto, para o backend receber exatamente 2 casas decimais.
        data: {'amount': amount.toStringAsFixed(2)},
      );
      return _unwrapPayment(response);
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<RidePaymentModel> confirmPayment(
    String rideId,
    String passengerId,
  ) async {
    try {
      final response = await _dio.post<dynamic>(
        RideEndpoints.confirmPayment(rideId, passengerId),
      );
      return _unwrapPayment(response);
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

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

  RidePaymentModel _unwrapPayment(Response<dynamic> response) =>
      unwrapApiResponse<RidePaymentModel>(
        response.data,
        RidePaymentModel.fromJson,
        status: response.statusCode ?? 200,
      );

  static String _isoDate(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';
}
