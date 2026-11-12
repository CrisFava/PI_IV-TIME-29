// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../../../auth/data/models/participation_role.dart';
import '../models/create_review_request.dart';
import '../models/review_model.dart';
import 'review_endpoints.dart';
import 'review_repository.dart';

final reviewRepositoryProvider = Provider<ReviewRepository>(
  (ref) => ReviewRepositoryImpl(ref.watch(dioProvider)),
);

class ReviewRepositoryImpl implements ReviewRepository {
  final Dio _dio;

  ReviewRepositoryImpl(this._dio);

  @override
  Future<ReviewModel> createReview(CreateReviewRequest request) async {
    try {
      final response = await _dio.post<dynamic>(
        ReviewEndpoints.reviews,
        data: request.toJson(),
      );
      return unwrapApiResponse<ReviewModel>(
        response.data,
        ReviewModel.fromJson,
        status: response.statusCode ?? 201,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<List<ReviewModel>> listGivenInRide(String rideId) =>
      _list(ReviewEndpoints.given, {'rideId': rideId});

  @override
  Future<List<ReviewModel>> listReceived(
    String userId, {
    required ParticipationRole role,
  }) => _list(ReviewEndpoints.received(userId), {'role': role.toJson()});

  Future<List<ReviewModel>> _list(
    String path,
    Map<String, dynamic> query,
  ) async {
    try {
      final response = await _dio.get<dynamic>(path, queryParameters: query);
      return unwrapApiListResponse<ReviewModel>(
        response.data,
        ReviewModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
