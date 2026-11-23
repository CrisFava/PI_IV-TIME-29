// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/ride_history_filter.dart';
import '../models/ride_history_item.dart';

final rideHistoryRepositoryProvider = Provider<RideHistoryRepository>(
  (ref) => RideHistoryRepository(ref.watch(dioProvider)),
);

/// Histórico de viagens (`GET /rides/history`).
class RideHistoryRepository {
  static const String _path = '/rides/history';

  final Dio _dio;

  RideHistoryRepository(this._dio);

  Future<List<RideHistoryItem>> list(
    RideHistoryFilter filter,
    DateTime today,
  ) async {
    final lastDays = filter.lastDays;
    try {
      final response = await _dio.get<dynamic>(
        _path,
        queryParameters: {
          'role': ?filter.role?.toJson(),
          if (lastDays != null)
            'from': _isoDate(today.subtract(Duration(days: lastDays))),
        },
      );
      return unwrapApiListResponse<RideHistoryItem>(
        response.data,
        RideHistoryItem.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  static String _isoDate(DateTime date) =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
