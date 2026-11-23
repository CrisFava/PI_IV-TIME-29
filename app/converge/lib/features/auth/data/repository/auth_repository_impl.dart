// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_response.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/network/api_exception_x.dart';
import '../../../../core/network/interceptors/auth_interceptor.dart';
import '../../../../core/services/auth/auth_exception.dart';
import '../models/cpf_validation_result.dart';
import '../models/create_user_request.dart';
import '../models/driver_license_input.dart';
import '../models/pix_key_type.dart';
import '../models/user_model.dart';
import 'auth_endpoints.dart';
import 'auth_repository.dart';

/// Provedor global da instância de [AuthRepository].
final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepositoryImpl(ref.watch(dioProvider)),
);

/// Implementação do repositório de autenticação e perfil de usuário utilizando Dio.
///
/// Todos os métodos seguem o mesmo padrão: chamam a rota, desempacotam o envelope `ApplicationResponse`
/// e trocam o `DioException` pela [ApiException] que o interceptor colocou nele.
class AuthRepositoryImpl implements AuthRepository {
  /// Cliente HTTP com os interceptors de autenticação, idempotência e erro.
  final Dio _dio;

  AuthRepositoryImpl(this._dio);

  /// `GET /users/me`.
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

  /// `POST /users` com o corpo do [CreateUserRequest].
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

  /// `PUT /users/me`. Campos opcionais nulos não vão no corpo, para o backend manter o valor atual.
  /// Se a conta logada mudou no meio, a [AuthException] do interceptor é repassada como está.
  @override
  Future<UserModel> updateProfile({
    required String name,
    required String phone,
    String? photoUrl,
    String? pixKey,
    PixKeyType? pixKeyType,
    String? expectedFirebaseUid,
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
        options: Options(
          extra: {AuthInterceptor.expectedUidKey: ?expectedFirebaseUid},
        ),
      );
      return unwrapApiResponse<UserModel>(
        response.data,
        UserModel.fromJson,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      final cause = e.error;
      if (cause is AuthException) throw cause;
      throw e.toApiException();
    }
  }

  /// `POST /users/me/driver-license`. A validade vai como data e hora local à meia-noite
  /// (`yyyy-MM-ddT00:00:00`), o formato do `LocalDateTime` do backend. Ainda não há envio da foto.
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

  /// `POST /users/me/driver`, sem corpo.
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

  /// `POST /users/validate-cpf` com o CPF como digitado; o servidor de sockets confere os dígitos verificadores.
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

  /// `GET /users/email-availability` com o e-mail sem espaços nas pontas.
  @override
  Future<bool> isEmailAvailable(String email) async {
    try {
      final response = await _dio.get<dynamic>(
        AuthEndpoints.emailAvailability,
        queryParameters: {'email': email.trim()},
      );
      return unwrapApiResponse<bool>(
        response.data,
        (json) => json['available'] as bool? ?? false,
        status: response.statusCode ?? 200,
      );
    } on DioException catch (e) {
      throw e.toApiException();
    }
  }
}
