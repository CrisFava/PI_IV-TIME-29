// Autor: Cristian Eduardo Fava - RA 25000636

import 'dart:convert';

import 'package:dio/dio.dart';

import '../../services/auth/auth_exception.dart';
import '../../services/auth/auth_service.dart';

/// Injeta `Authorization: Bearer <Firebase ID token>` e, se o backend
/// responder 401, força a renovação do token e repete a requisição uma vez.
///
/// Requisições com `extra[expectedUidKey]` só são enviadas com um token
/// emitido para essa conta; se a sessão ativa for outra, a requisição é
/// rejeitada com `AuthException('session-changed')` sem ser despachada.
class AuthInterceptor extends Interceptor {
  static const String _retriedFlag = 'auth_retried';

  /// Chave em `RequestOptions.extra` com o Firebase UID esperado.
  static const String expectedUidKey = 'auth_expected_uid';

  final AuthService _authService;
  final Dio _dio;

  AuthInterceptor(this._authService, this._dio);

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    try {
      // Se a requisição já é um retry com Authorization configurado, não sobrescreve
      if (options.extra[_retriedFlag] == true &&
          options.headers.containsKey('Authorization')) {
        return handler.next(options);
      }

      final expectedUid = _expectedUid(options);
      var token = await _authService.getIdToken(expectedUid: expectedUid);

      // Se o token existir e já estiver expirado ou perto de expirar (< 60s),
      // força a renovação antes de enviar ao backend para evitar rejeição com 401.
      if (token != null && _isTokenExpiredOrExpiring(token)) {
        token = await _authService.getIdToken(
          forceRefresh: true,
          expectedUid: expectedUid,
        );
      }

      _assertTokenBelongsTo(token, expectedUid);

      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    } catch (e) {
      handler.reject(DioException(requestOptions: options, error: e));
    }
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final options = err.requestOptions;
    final alreadyRetried = options.extra[_retriedFlag] == true;
    if (err.response?.statusCode != 401 || alreadyRetried) {
      return handler.next(err);
    }

    try {
      final expectedUid = _expectedUid(options);
      final token = await _authService.getIdToken(
        forceRefresh: true,
        expectedUid: expectedUid,
      );
      if (token == null) return handler.next(err);
      _assertTokenBelongsTo(token, expectedUid);

      options.extra[_retriedFlag] = true;
      options.headers['Authorization'] = 'Bearer $token';
      handler.resolve(await _dio.fetch<dynamic>(options));
    } on DioException catch (retryError) {
      handler.next(retryError);
    } on AuthException catch (authError) {
      handler.next(DioException(requestOptions: options, error: authError));
    } catch (_) {
      handler.next(err);
    }
  }

  String? _expectedUid(RequestOptions options) {
    final value = options.extra[expectedUidKey];
    return value is String ? value : null;
  }

  /// Garante que o token resolve para a conta esperada (claim `user_id`/`sub`).
  void _assertTokenBelongsTo(String? token, String? expectedUid) {
    if (expectedUid == null) return;
    if (token == null || _tokenUid(token) != expectedUid) {
      throw AuthException.sessionChanged();
    }
  }

  /// Extrai o Firebase UID do payload do JWT, ou nulo se não for possível.
  String? _tokenUid(String token) {
    final payload = _decodePayload(token);
    if (payload == null) return null;
    final uid = payload['user_id'] ?? payload['sub'];
    return uid is String ? uid : null;
  }

  Map<String, dynamic>? _decodePayload(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return null;
      final normalized = base64Url.normalize(parts[1]);
      final payload = jsonDecode(utf8.decode(base64Url.decode(normalized)));
      return payload is Map<String, dynamic> ? payload : null;
    } catch (_) {
      return null;
    }
  }

  /// Verifica se o token JWT está expirado ou se expira em menos de 60 segundos.
  /// Para tokens de teste/fakes (sem 3 partes separadas por ponto), retorna false.
  bool _isTokenExpiredOrExpiring(String token) {
    final exp = _decodePayload(token)?['exp'];
    if (exp is! num) return false;
    final nowInSeconds = DateTime.now().toUtc().millisecondsSinceEpoch ~/ 1000;
    return nowInSeconds >= (exp.toInt() - 60);
  }
}
