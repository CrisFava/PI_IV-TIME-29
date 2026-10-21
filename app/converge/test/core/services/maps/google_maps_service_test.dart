import 'package:converge/core/services/maps/google_maps_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

void main() {
  group('GoogleMapsService - Routes API', () {
    late Dio mockDio;

    setUp(() {
      mockDio = Dio();
      GoogleMapsService.testDio = mockDio;
    });

    tearDown(() {
      GoogleMapsService.testDio = null;
    });

    test('Retorna rota viária decodificada quando Routes API responder com encodedPolyline', () async {
      // Polyline canônica do Google: (38.5, -120.2) -> (40.7, -120.95)
      const testPolyline = '_p~iF~ps|U_ulLnnqC';

      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            // Valida headers exigidos pela Routes API
            expect(options.headers['X-Goog-Api-Key'], isNotEmpty);
            expect(options.headers['X-Goog-FieldMask'], contains('routes.polyline.encodedPolyline'));
            expect(options.method, 'POST');
            expect(options.path, contains('computeRoutes'));

            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {
                  'routes': [
                    {
                      'polyline': {
                        'encodedPolyline': testPolyline,
                      },
                    },
                  ],
                },
              ),
            );
          },
        ),
      );

      final route = await GoogleMapsService.computeRoutes(
        origin: const LatLng(38.5, -120.2),
        destination: const LatLng(40.7, -120.95),
      );

      expect(route, isNotEmpty);
      expect(route.length, 2);
      expect(route.first.latitude, closeTo(38.5, 0.001));
      expect(route.first.longitude, closeTo(-120.2, 0.001));
    });

    test('Lança RoutesException com ZERO_RESULTS quando lista de rotas for vazia', () async {
      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {
                  'routes': [],
                },
              ),
            );
          },
        ),
      );

      expect(
        () => GoogleMapsService.computeRoutes(
          origin: const LatLng(0, 0),
          destination: const LatLng(10, 10),
        ),
        throwsA(
          isA<RoutesException>()
              .having((e) => e.apiStatus, 'apiStatus', 'ZERO_RESULTS')
              .having(
                (e) => e.message,
                'message',
                contains('Nenhuma rota viária encontrada'),
              ),
        ),
      );
    });

    test('Lança RoutesException com diagnóstico quando erro RPC for PERMISSION_DENIED', () async {
      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.reject(
              DioException(
                requestOptions: options,
                type: DioExceptionType.badResponse,
                response: Response(
                  requestOptions: options,
                  statusCode: 403,
                  data: {
                    'error': {
                      'code': 403,
                      'message': 'Routes API has not been used in project before.',
                      'status': 'PERMISSION_DENIED',
                    },
                  },
                ),
              ),
            );
          },
        ),
      );

      expect(
        () => GoogleMapsService.computeRoutes(
          origin: const LatLng(-22.8330, -47.0510),
          destination: const LatLng(-22.8340, -47.0520),
        ),
        throwsA(
          isA<RoutesException>()
              .having((e) => e.apiStatus, 'apiStatus', 'PERMISSION_DENIED')
              .having((e) => e.apiErrorMessage, 'apiErrorMessage', contains('Routes API has not been used'))
              .having(
                (e) => e.message,
                'message',
                contains('Acesso negado à Routes API'),
              ),
        ),
      );
    });

    test('Lança RoutesException quando status RPC for RESOURCE_EXHAUSTED', () async {
      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.reject(
              DioException(
                requestOptions: options,
                type: DioExceptionType.badResponse,
                response: Response(
                  requestOptions: options,
                  statusCode: 429,
                  data: {
                    'error': {
                      'code': 429,
                      'message': 'Quota exceeded for Routes API.',
                      'status': 'RESOURCE_EXHAUSTED',
                    },
                  },
                ),
              ),
            );
          },
        ),
      );

      expect(
        () => GoogleMapsService.computeRoutes(
          origin: const LatLng(-22.8330, -47.0510),
          destination: const LatLng(-22.8340, -47.0520),
        ),
        throwsA(
          isA<RoutesException>()
              .having((e) => e.apiStatus, 'apiStatus', 'RESOURCE_EXHAUSTED')
              .having(
                (e) => e.message,
                'message',
                contains('Limite de requisições'),
              ),
        ),
      );
    });

    test('Lança RoutesException em caso de timeout de conexão Dio', () async {
      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.reject(
              DioException(
                requestOptions: options,
                type: DioExceptionType.connectionTimeout,
              ),
            );
          },
        ),
      );

      expect(
        () => GoogleMapsService.computeRoutes(
          origin: const LatLng(-22.8330, -47.0510),
          destination: const LatLng(-22.8340, -47.0520),
        ),
        throwsA(
          isA<RoutesException>().having(
            (e) => e.message,
            'message',
            contains('Tempo limite esgotado'),
          ),
        ),
      );
    });

    test('Lança RoutesException quando encodedPolyline estiver vazio ou ausente', () async {
      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {
                  'routes': [
                    {'polyline': {'encodedPolyline': ''}},
                  ],
                },
              ),
            );
          },
        ),
      );

      expect(
        () => GoogleMapsService.computeRoutes(
          origin: const LatLng(-22.8330, -47.0510),
          destination: const LatLng(-22.8340, -47.0520),
        ),
        throwsA(
          isA<RoutesException>().having(
            (e) => e.message,
            'message',
            contains('não possui pontos de traçado'),
          ),
        ),
      );
    });

    test('decodePolyline decodifica corretamente e rejeita strings truncadas', () {
      const validPoly = '_p~iF~ps|U_ulLnnqC_mqNvxq`@';
      final points = GoogleMapsService.decodePolyline(validPoly);
      expect(points.length, 3);

      expect(
        () => GoogleMapsService.decodePolyline('___'),
        throwsA(isA<FormatException>()),
      );
    });
  });
}
