import 'package:converge/core/services/maps/google_maps_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

void main() {
  group('GoogleMapsService - Routes e Geocoding Visual', () {
    late Dio mockDio;

    setUp(() {
      mockDio = Dio();
      GoogleMapsService.testDio = mockDio;
      dotenv.loadFromString(envString: 'GOOGLE_MAPS_API_KEY=test_mock_key');
    });

    tearDown(() {
      GoogleMapsService.testDio = null;
      dotenv.clean();
    });

    test('decodePolyline decodifica corretamente e rejeita strings truncadas', () {
      // Polyline canônica do Google: (38.5, -120.2) -> (40.7, -120.95)
      const testPolyline = '_p~iF~ps|U_ulLnnqC';
      final points = GoogleMapsService.decodePolyline(testPolyline);

      expect(points.length, 2);
      expect(points[0].latitude, closeTo(38.5, 0.001));
      expect(points[0].longitude, closeTo(-120.2, 0.001));
      expect(points[1].latitude, closeTo(40.7, 0.001));
      expect(points[1].longitude, closeTo(-120.95, 0.001));

      expect(
        () => GoogleMapsService.decodePolyline('_p~iF~ps|'),
        throwsA(isA<FormatException>()),
      );
    });

    test('computeRoutes prioriza encodedPolyline quando fornecido', () async {
      const testPolyline = '_p~iF~ps|U_ulLnnqC';
      final route = await GoogleMapsService.computeRoutes(
        origin: const LatLng(38.5, -120.2),
        destination: const LatLng(40.7, -120.95),
        encodedPolyline: testPolyline,
      );

      expect(route.length, 2);
      expect(route.first.latitude, closeTo(38.5, 0.001));
    });

    test('computeRoutes busca rota viária pelas ruas quando não há encodedPolyline', () async {
      const testPolyline = '_p~iF~ps|U_ulLnnqC';

      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.headers['X-Goog-Api-Key'], 'test_mock_key');
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

      expect(route.length, 2);
      expect(route.first.latitude, closeTo(38.5, 0.001));
    });

    test('computeRoutes lança RoutesException com MISSING_API_KEY se chave ausente', () async {
      dotenv.clean();

      expect(
        () => GoogleMapsService.computeRoutes(
          origin: const LatLng(0, 0),
          destination: const LatLng(1, 1),
        ),
        throwsA(
          isA<RoutesException>()
              .having((e) => e.apiStatus, 'apiStatus', 'MISSING_API_KEY'),
        ),
      );
    });

    test('getAddressFromCoordinates retorna endereço amigável', () async {
      const point = LatLng(-22.8315, -47.0512);

      mockDio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {
                  'status': 'OK',
                  'results': [
                    {
                      'formatted_address': 'Av. Rebouças, 100 - Centro, Campinas - SP',
                    },
                  ],
                },
              ),
            );
          },
        ),
      );

      final address = await GoogleMapsService.getAddressFromCoordinates(point);
      expect(address, contains('Av. Rebouças'));
    });
  });
}
