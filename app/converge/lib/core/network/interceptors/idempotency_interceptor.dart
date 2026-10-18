import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';

/// Injeta `Idempotency-Key` (UUID v4) em métodos com efeito colateral.
///
/// A chave fica em `options.headers`, então um retry do mesmo
/// [RequestOptions] reaproveita a mesma chave.
class IdempotencyInterceptor extends Interceptor {
  static const String header = 'Idempotency-Key';
  static const Set<String> _mutatingMethods = {'POST', 'PUT', 'PATCH'};

  final Uuid _uuid = const Uuid();

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (_mutatingMethods.contains(options.method.toUpperCase())) {
      options.headers.putIfAbsent(header, () => _uuid.v4());
    }
    handler.next(options);
  }
}
