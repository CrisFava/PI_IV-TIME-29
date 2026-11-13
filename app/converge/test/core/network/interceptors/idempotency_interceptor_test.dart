// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:converge/core/network/interceptors/idempotency_interceptor.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

class _TestRequestHandler extends RequestInterceptorHandler {
  RequestOptions? nextOptions;

  @override
  void next(RequestOptions requestOptions) {
    nextOptions = requestOptions;
  }
}

void main() {
  final uuidV4Pattern = RegExp(
    r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
    caseSensitive: false,
  );

  late IdempotencyInterceptor interceptor;

  setUp(() {
    interceptor = IdempotencyInterceptor();
  });

  for (final method in ['POST', 'PUT', 'PATCH']) {
    test('injeta Idempotency-Key UUID v4 em requisicoes $method', () {
      final options = RequestOptions(
        path: '/api/v1/rides/request',
        method: method,
      );
      final handler = _TestRequestHandler();

      interceptor.onRequest(options, handler);

      final key = handler.nextOptions?.headers[IdempotencyInterceptor.header];
      expect(key, isA<String>());
      expect(uuidV4Pattern.hasMatch(key as String), isTrue);
    });
  }

  test('reaproveita Idempotency-Key existente em retries', () {
    const existingKey = '11111111-1111-4111-8111-111111111111';
    final options = RequestOptions(
      path: '/api/v1/rides/ride-1/passengers/p1/accept',
      method: 'POST',
      headers: {IdempotencyInterceptor.header: existingKey},
    );
    final handler = _TestRequestHandler();

    interceptor.onRequest(options, handler);

    expect(
      handler.nextOptions?.headers[IdempotencyInterceptor.header],
      equals(existingKey),
    );
  });

  for (final method in ['GET', 'DELETE']) {
    test('nao injeta Idempotency-Key em requisicoes $method', () {
      final options = RequestOptions(
        path: '/api/v1/rides',
        method: method,
      );
      final handler = _TestRequestHandler();

      interceptor.onRequest(options, handler);

      expect(
        handler.nextOptions?.headers.containsKey(IdempotencyInterceptor.header),
        isFalse,
      );
    });
  }
}
