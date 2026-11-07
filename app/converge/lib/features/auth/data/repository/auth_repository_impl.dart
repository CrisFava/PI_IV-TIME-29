// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/network/api_exception_x.dart';
import '../models/cpf_validation_result.dart';
import '../models/create_user_request.dart';
import '../models/driver_license_input.dart';
import '../models/pix_key_type.dart';
import '../models/user_model.dart';
import 'auth_endpoints.dart';
import 'auth_repository.dart';

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepositoryImpl(ref.watch(dioProvider)),
);

class AuthRepositoryImpl implements AuthRepository {
  final Dio _dio;

  AuthRepositoryImpl(this._dio);

  @override
  Future<UserModel> fetchCurrentUser() async {
    try {
      final response = await _dio.get<dynamic>(AuthEndpoints.currentUser);
      return unwrapApiResponse<UserModel>(
        response.data,
        UserModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<UserModel> createUser(CreateUserRequest request) async {
    try {
      final response = await _dio.post<dynamic>(
        AuthEndpoints.createUser,
        data: request.toJson(),
      );
      return unwrapApiResponse<UserModel>(
        response.data,
        UserModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<UserModel> updateProfile({
    required String name,
    required String phone,
    String? photoUrl,
    String? pixKey,
    PixKeyType? pixKeyType,
  }) async {
    try {
      final response = await _dio.put<dynamic>(
        AuthEndpoints.currentUser,
        data: {
          'name': name,
          'phone': phone,
          'photoUrl': ?photoUrl,
          'pixKey': ?pixKey,
          'pixKeyType': ?pixKeyType?.toJson(),
        },
      );
      return unwrapApiResponse<UserModel>(
        response.data,
        UserModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<UserModel> submitDriverLicense(DriverLicenseInput license) async {
    try {
      final formattedExpiresAt =
          '${license.expiresAt.year.toString().padLeft(4, '0')}-'
          '${license.expiresAt.month.toString().padLeft(2, '0')}-'
          '${license.expiresAt.day.toString().padLeft(2, '0')}T00:00:00';

      final response = await _dio.post<dynamic>(
        AuthEndpoints.submitDriverLicense,
        data: {
          'number': license.number,
          'category': license.category.toJson(),
          'expiresAt': formattedExpiresAt,
          'documentUrl': null,
        },
      );
      return unwrapApiResponse<UserModel>(
        response.data,
        UserModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<UserModel> becomeDriver() async {
    try {
      final response = await _dio.post<dynamic>(AuthEndpoints.becomeDriver);
      return unwrapApiResponse<UserModel>(
        response.data,
        UserModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }

  @override
  Future<CpfValidationResult> validateCpf(String cpf) async {
    try {
      final response = await _dio.post<dynamic>(
        AuthEndpoints.validateCpf,
        data: {'cpf': cpf},
      );
      return unwrapApiResponse<CpfValidationResult>(
        response.data,
        CpfValidationResult.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
