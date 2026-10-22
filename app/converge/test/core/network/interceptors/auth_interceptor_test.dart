import 'dart:convert';
import 'package:converge/core/network/interceptors/auth_interceptor.dart';
import 'package:converge/core/services/auth/auth_service.dart';
import 'package:converge/core/services/auth/auth_user.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

class _MockAuthService implements AuthService {
  String? currentToken;
  String? refreshedToken;
  bool forceRefreshCalled = false;
  AuthUser? user;

  _MockAuthService({this.currentToken, this.refreshedToken, this.user});

  @override
  Stream<AuthUser?> authStateChanges() => Stream.value(user);

  @override
  AuthUser? get currentUser => user;

  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async {
    if (forceRefresh) {
      forceRefreshCalled = true;
      return refreshedToken ?? currentToken;
    }
    return currentToken;
  }

  @override
  Future<AuthUser> signIn({required String email, required String password}) =>
      throw UnimplementedError();

  @override
  Future<AuthUser> signUp({required String email, required String password}) =>
      throw UnimplementedError();

  @override
  Future<void> sendPasswordResetEmail(String email) => throw UnimplementedError();

  @override
  Future<void> sendEmailVerification() => throw UnimplementedError();

  @override
  Future<void> signOut() => throw UnimplementedError();
}

class _TestRequestHandler extends RequestInterceptorHandler {
  RequestOptions? nextOptions;

  @override
  void next(RequestOptions requestOptions) {
    nextOptions = requestOptions;
  }
}

class _TestErrorHandler extends ErrorInterceptorHandler {
  DioException? nextError;
  Response? resolvedResponse;

  @override
  void next(DioException err) {
    nextError = err;
  }

  @override
  void resolve(Response response) {
    resolvedResponse = response;
  }
}

String _createJwt(int exp) {
  final header = base64Url.encode(utf8.encode(jsonEncode({'alg': 'none'})));
  final payload = base64Url
      .encode(utf8.encode(jsonEncode({'exp': exp, 'user_id': 'test_user'})));
  return '$header.$payload.sig';
}

void main() {
  group('AuthInterceptor', () {
    late Dio dio;

    setUp(() {
      dio = Dio();
    });

    test('onRequest injeta Authorization se token válido estiver disponível', () async {
      final nowSec = DateTime.now().toUtc().millisecondsSinceEpoch ~/ 1000;
      final validToken = _createJwt(nowSec + 3600); // Válido por 1 hora
      final auth = _MockAuthService(
        currentToken: validToken,
        user: const AuthUser(uid: 'u1', email: 'a@puc.br', emailVerified: true),
      );

      final interceptor = AuthInterceptor(auth, dio);
      final options = RequestOptions(path: '/api/v1/routes');
      final handler = _TestRequestHandler();

      await interceptor.onRequest(options, handler);

      expect(options.headers['Authorization'], 'Bearer $validToken');
      expect(auth.forceRefreshCalled, isFalse);
      expect(handler.nextOptions, isNotNull);
    });

    test('onRequest força renovação se o token estiver a menos de 60s de expirar', () async {
      final nowSec = DateTime.now().toUtc().millisecondsSinceEpoch ~/ 1000;
      final expiringToken = _createJwt(nowSec + 20); // Expira em 20s
      final freshToken = _createJwt(nowSec + 3600);

      final auth = _MockAuthService(
        currentToken: expiringToken,
        refreshedToken: freshToken,
        user: const AuthUser(uid: 'u1', email: 'a@puc.br', emailVerified: true),
      );

      final interceptor = AuthInterceptor(auth, dio);
      final options = RequestOptions(path: '/api/v1/routes');
      final handler = _TestRequestHandler();

      await interceptor.onRequest(options, handler);

      expect(auth.forceRefreshCalled, isTrue);
      expect(options.headers['Authorization'], 'Bearer $freshToken');
      expect(handler.nextOptions, isNotNull);
    });

    test('onRequest não sobrescreve Authorization se flag auth_retried estiver ativa', () async {
      final auth = _MockAuthService(
        currentToken: 'stale_token',
        user: const AuthUser(uid: 'u1', email: 'a@puc.br', emailVerified: true),
      );

      final interceptor = AuthInterceptor(auth, dio);
      final options = RequestOptions(
        path: '/api/v1/routes',
        headers: {'Authorization': 'Bearer fresh_retried_token'},
        extra: {'auth_retried': true},
      );
      final handler = _TestRequestHandler();

      await interceptor.onRequest(options, handler);

      expect(options.headers['Authorization'], 'Bearer fresh_retried_token');
      expect(handler.nextOptions, isNotNull);
    });

    test('onRequest não define Authorization se token for nulo', () async {
      final auth = _MockAuthService(currentToken: null, user: null);
      final interceptor = AuthInterceptor(auth, dio);
      final options = RequestOptions(path: '/api/v1/routes');
      final handler = _TestRequestHandler();

      await interceptor.onRequest(options, handler);

      expect(options.headers.containsKey('Authorization'), isFalse);
      expect(handler.nextOptions, isNotNull);
    });

    test('onError repassa erro imediatamente se statusCode não for 401', () async {
      final auth = _MockAuthService();
      final interceptor = AuthInterceptor(auth, dio);

      final options = RequestOptions(path: '/api/v1/routes');
      final err = DioException(
        requestOptions: options,
        response: Response(requestOptions: options, statusCode: 500),
      );

      final handler = _TestErrorHandler();
      await interceptor.onError(err, handler);

      expect(auth.forceRefreshCalled, isFalse);
      expect(handler.nextError, equals(err));
    });

    test('onError repassa erro se já tiver tentado retry anteriormente', () async {
      final auth = _MockAuthService();
      final interceptor = AuthInterceptor(auth, dio);

      final options = RequestOptions(
        path: '/api/v1/routes',
        extra: {'auth_retried': true},
      );
      final err = DioException(
        requestOptions: options,
        response: Response(requestOptions: options, statusCode: 401),
      );

      final handler = _TestErrorHandler();
      await interceptor.onError(err, handler);

      expect(auth.forceRefreshCalled, isFalse);
      expect(handler.nextError, equals(err));
    });

    test('onError repassa erro se forceRefresh retornar token nulo', () async {
      final auth = _MockAuthService(currentToken: null, refreshedToken: null);
      final interceptor = AuthInterceptor(auth, dio);

      final options = RequestOptions(path: '/api/v1/routes');
      final err = DioException(
        requestOptions: options,
        response: Response(requestOptions: options, statusCode: 401),
      );

      final handler = _TestErrorHandler();
      await interceptor.onError(err, handler);

      expect(auth.forceRefreshCalled, isTrue);
      expect(handler.nextError, equals(err));
    });
  });
}
