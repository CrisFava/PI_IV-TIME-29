// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../models/block_model.dart';
import '../models/create_report_request.dart';
import '../models/report_model.dart';
import 'security_endpoints.dart';
import 'security_repository.dart';

final securityRepositoryProvider = Provider<SecurityRepository>(
  (ref) => SecurityRepositoryImpl(ref.watch(dioProvider)),
);

class SecurityRepositoryImpl implements SecurityRepository {
  final Dio _dio;

  SecurityRepositoryImpl(this._dio);

  @override
  Future<BlockModel> blockUser(String blockedUserId) async {
    try {
      final response = await _dio.post<dynamic>(
        SecurityEndpoints.blocks,
        data: {'blockedUserId': blockedUserId},
      );
      return unwrapApiResponse<BlockModel>(
        response.data,
        BlockModel.fromJson,
        status: response.statusCode ?? 201,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<void> unblockUser(String blockedUserId) async {
    try {
      // O backend responde 204, sem corpo.
      await _dio.delete<dynamic>(SecurityEndpoints.block(blockedUserId));
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<List<BlockModel>> listBlocks() async {
    try {
      final response = await _dio.get<dynamic>(SecurityEndpoints.blocks);
      return unwrapApiListResponse<BlockModel>(
        response.data,
        BlockModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<ReportModel> createReport(CreateReportRequest request) async {
    try {
      final response = await _dio.post<dynamic>(
        SecurityEndpoints.reports,
        data: request.toJson(),
      );
      return unwrapApiResponse<ReportModel>(
        response.data,
        ReportModel.fromJson,
        status: response.statusCode ?? 201,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<List<ReportModel>> listMyReports() async {
    try {
      final response = await _dio.get<dynamic>(SecurityEndpoints.myReports);
      return unwrapApiListResponse<ReportModel>(
        response.data,
        ReportModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
