// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';

/// Injeta `Idempotency-Key` (UUID v4) em métodos com efeito colateral.
///
/// A chave fica em `options.headers`, então um retry do mesmo
/// [RequestOptions] reaproveita a mesma chave.
class IdempotencyInterceptor extends Interceptor {
  /// Nome do cabeçalho lido pelo backend (RNF21).
  static const String header = 'Idempotency-Key';

  /// Métodos que alteram dados. GET e DELETE não recebem a chave.
  static const Set<String> _mutatingMethods = {'POST', 'PUT', 'PATCH'};

  /// Gerador de UUID v4.
  final Uuid _uuid = const Uuid();

  /// Coloca a chave nas requisições POST, PUT e PATCH que ainda não têm uma. Uma chave enviada pelo
  /// próprio chamador é mantida.
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (_mutatingMethods.contains(options.method.toUpperCase())) {
      options.headers.putIfAbsent(header, () => _uuid.v4());
    }
    handler.next(options);
  }
}
