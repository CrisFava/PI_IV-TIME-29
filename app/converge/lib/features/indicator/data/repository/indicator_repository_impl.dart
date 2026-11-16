// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/indicator_period.dart';
import '../models/methodology_item.dart';
import '../models/platform_indicator_model.dart';
import '../models/user_indicator_model.dart';
import 'indicator_endpoints.dart';
import 'indicator_repository.dart';

final indicatorRepositoryProvider = Provider<IndicatorRepository>(
  (ref) => IndicatorRepositoryImpl(ref.watch(dioProvider)),
);

class IndicatorRepositoryImpl implements IndicatorRepository {
  final Dio _dio;

  IndicatorRepositoryImpl(this._dio);

  @override
  Future<UserIndicatorModel> myIndicators(IndicatorPeriod period) =>
      _get(IndicatorEndpoints.me, period, UserIndicatorModel.fromJson);

  @override
  Future<PlatformIndicatorModel> platformIndicators(IndicatorPeriod period) =>
      _get(
        IndicatorEndpoints.platform,
        period,
        PlatformIndicatorModel.fromJson,
      );

  @override
  Future<List<MethodologyItem>> methodology() async {
    try {
      final response = await _dio.get<dynamic>(IndicatorEndpoints.methodology);
      return unwrapApiResponse<List<MethodologyItem>>(
        response.data,
        MethodologyItem.listFromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  Future<T> _get<T>(
    String path,
    IndicatorPeriod period,
    T Function(Map<String, dynamic> json) fromJson,
  ) async {
    try {
      final response = await _dio.get<dynamic>(
        path,
        queryParameters: {
          'period': period.apiValue,
          // O período atual (sem a referência, o backend pega o último com dados)
          'reference': ?period.referenceFor(DateTime.now()),
        },
      );
      return unwrapApiResponse<T>(
        response.data,
        fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
