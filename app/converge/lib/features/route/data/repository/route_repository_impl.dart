import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/create_route_request.dart';
import '../models/place_details_model.dart';
import '../models/place_suggestion_model.dart';
import '../models/route_response_model.dart';
import 'route_endpoints.dart';
import 'route_repository.dart';

final routeRepositoryProvider = Provider<RouteRepository>(
  (ref) => RouteRepositoryImpl(ref.watch(dioProvider)),
);

class RouteRepositoryImpl implements RouteRepository {
  final Dio _dio;

  RouteRepositoryImpl(this._dio);

  @override
  Future<List<PlaceSuggestionModel>> autocomplete({
    required String input,
    String? sessionToken,
  }) async {
    try {
      final response = await _dio.get<dynamic>(
        RouteEndpoints.placesAutocomplete,
        queryParameters: {
          'input': input,
          'sessionToken': ?sessionToken,
        },
      );
      return unwrapApiListResponse<PlaceSuggestionModel>(
        response.data,
        PlaceSuggestionModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<PlaceDetailsModel> getPlaceDetails({
    required String placeId,
    String? sessionToken,
  }) async {
    try {
      final response = await _dio.get<dynamic>(
        RouteEndpoints.placeDetails(placeId),
        queryParameters: {
          'sessionToken': ?sessionToken,
        },
      );
      return unwrapApiResponse<PlaceDetailsModel>(
        response.data,
        PlaceDetailsModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<RouteResponseModel> createRoute(CreateRouteRequest request) async {
    try {
      final response = await _dio.post<dynamic>(
        RouteEndpoints.routes,
        data: request.toJson(),
      );
      return unwrapApiResponse<RouteResponseModel>(
        response.data,
        RouteResponseModel.fromJson,
        status: response.statusCode ?? 201,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<List<RouteResponseModel>> listMyRoutes() async {
    try {
      final response = await _dio.get<dynamic>(RouteEndpoints.routes);
      return unwrapApiListResponse<RouteResponseModel>(
        response.data,
        RouteResponseModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<RouteResponseModel> getRoute(String routeId) async {
    try {
      final response = await _dio.get<dynamic>(RouteEndpoints.routeById(routeId));
      return unwrapApiResponse<RouteResponseModel>(
        response.data,
        RouteResponseModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<void> deleteRoute(String routeId) async {
    try {
      await _dio.delete<dynamic>(RouteEndpoints.routeById(routeId));
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
