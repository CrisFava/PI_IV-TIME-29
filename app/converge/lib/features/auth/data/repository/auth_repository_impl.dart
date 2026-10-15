import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/network/problem_details_x.dart';
import '../models/create_user_request.dart';
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
      return unwrapApiResponse<UserModel>(response.data, UserModel.fromJson);
    } on DioException catch (e) {
      throw e.toProblem();
    }
  }

  @override
  Future<UserModel> createUser(CreateUserRequest request) async {
    try {
      final response = await _dio.post<dynamic>(
        AuthEndpoints.createUser,
        data: request.toJson(),
      );
      return unwrapApiResponse<UserModel>(response.data, UserModel.fromJson);
    } on DioException catch (e) {
      throw e.toProblem();
    }
  }
}
