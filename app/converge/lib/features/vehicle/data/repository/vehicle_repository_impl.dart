// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/vehicle_summary_model.dart';
import 'vehicle_endpoints.dart';
import 'vehicle_repository.dart';

final vehicleRepositoryProvider = Provider<VehicleRepository>(
  (ref) => VehicleRepositoryImpl(ref.watch(dioProvider)),
);

class VehicleRepositoryImpl implements VehicleRepository {
  final Dio _dio;

  VehicleRepositoryImpl(this._dio);

  @override
  Future<List<VehicleSummaryModel>> listMyVehicles() async {
    try {
      final response = await _dio.get<dynamic>(VehicleEndpoints.vehicles);
      return unwrapApiListResponse<VehicleSummaryModel>(
        response.data,
        VehicleSummaryModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
