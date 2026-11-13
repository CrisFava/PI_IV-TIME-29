// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/create_vehicle_request.dart';
import '../models/vehicle_model.dart';
import '../validation/vehicle_validator.dart';
import 'vehicle_endpoints.dart';
import 'vehicle_repository.dart';

final vehicleRepositoryProvider = Provider<VehicleRepository>(
  (ref) => VehicleRepositoryImpl(ref.watch(dioProvider)),
);

class VehicleRepositoryImpl implements VehicleRepository {
  final Dio _dio;

  VehicleRepositoryImpl(this._dio);

  @override
  Future<List<VehicleModel>> listMyVehicles() async {
    try {
      final response = await _dio.get<dynamic>(VehicleEndpoints.vehicles);
      return unwrapApiListResponse<VehicleModel>(
        response.data,
        VehicleModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<VehicleModel> createVehicle(CreateVehicleRequest request) async {
    // Dados inválidos nem chegam à API
    VehicleValidator.validate(request);

    try {
      final response = await _dio.post<dynamic>(
        VehicleEndpoints.vehicles,
        data: request.toJson(),
      );
      return unwrapApiResponse<VehicleModel>(
        response.data,
        VehicleModel.fromJson,
        status: response.statusCode ?? 201,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<VehicleModel> updateVehicle(
    String vehicleId,
    CreateVehicleRequest request,
  ) async {
    VehicleValidator.validate(request);

    try {
      final response = await _dio.put<dynamic>(
        VehicleEndpoints.vehicleById(vehicleId),
        data: request.toJson(),
      );
      return unwrapApiResponse<VehicleModel>(
        response.data,
        VehicleModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<void> deleteVehicle(String vehicleId) async {
    try {
      await _dio.delete<dynamic>(VehicleEndpoints.vehicleById(vehicleId));
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
