// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/match_model.dart';
import 'matching_endpoints.dart';
import 'matching_repository.dart';

final matchingRepositoryProvider = Provider<MatchingRepository>(
  (ref) => MatchingRepositoryImpl(ref.watch(dioProvider)),
);

class MatchingRepositoryImpl implements MatchingRepository {
  final Dio _dio;

  MatchingRepositoryImpl(this._dio);

  @override
  Future<List<MatchModel>> findDriversFor(String searchRouteId) =>
      _find(MatchingEndpoints.passenger, searchRouteId);

  @override
  Future<List<MatchModel>> findPassengersFor(String offerRouteId) =>
      _find(MatchingEndpoints.driver, offerRouteId);

  Future<List<MatchModel>> _find(String path, String routeId) async {
    try {
      final response = await _dio.get<dynamic>(
        path,
        queryParameters: {'routeId': routeId},
      );
      return unwrapApiListResponse<MatchModel>(
        response.data,
        MatchModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
