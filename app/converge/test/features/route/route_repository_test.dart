import 'package:converge/features/route/data/models/create_route_request.dart';
import 'package:converge/features/route/data/models/route_place_model.dart';
import 'package:converge/features/route/data/repository/route_endpoints.dart';
import 'package:converge/features/route/data/repository/route_repository_impl.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('RouteRepositoryImpl', () {
    late Dio dio;
    late RouteRepositoryImpl repository;

    setUp(() {
      dio = Dio();
      repository = RouteRepositoryImpl(dio);
    });

    test('autocomplete retorna lista de PlaceSuggestionModel extraída do envelope', () async {
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.path, RouteEndpoints.placesAutocomplete);
            expect(options.queryParameters['input'], 'PUC Campinas');
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {
                  'success': true,
                  'data': [
                    {
                      'placeId': 'place_123',
                      'mainText': 'PUC-Campinas - Campus I',
                      'secondaryText': 'R. Prof. Dr. Edivaldo Pereira Falcão',
                    },
                  ],
                  'error': null,
                  'timestamp': '2026-10-22T18:00:00Z',
                },
              ),
            );
          },
        ),
      );

      final suggestions = await repository.autocomplete(input: 'PUC Campinas');
      expect(suggestions.length, 1);
      expect(suggestions.first.placeId, 'place_123');
      expect(suggestions.first.mainText, 'PUC-Campinas - Campus I');
    });

    test('getPlaceDetails retorna PlaceDetailsModel com coordenadas', () async {
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.path, RouteEndpoints.placeDetails('place_123'));
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {
                  'success': true,
                  'data': {
                    'address': 'Rua Professor Doutor Edivaldo, 123',
                    'neighborhood': 'Parque das Universidades',
                    'city': 'Campinas',
                    'latitude': -22.8315,
                    'longitude': -47.0512,
                  },
                  'error': null,
                  'timestamp': '2026-10-22T18:00:00Z',
                },
              ),
            );
          },
        ),
      );

      final details = await repository.getPlaceDetails(placeId: 'place_123');
      expect(details.city, 'Campinas');
      expect(details.latitude, -22.8315);
      expect(details.longitude, -47.0512);
    });

    test('createRoute envia payload correto e recebe RouteResponseModel com polyline', () async {
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.path, RouteEndpoints.routes);
            expect(options.method, 'POST');
            final data = options.data as Map<String, dynamic>;
            expect(data['type'], 'SEARCH');
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 201,
                data: {
                  'success': true,
                  'data': {
                    'id': 'route_999',
                    'type': 'SEARCH',
                    'status': 'ACTIVE',
                    'origin': {
                      'address': 'PUC',
                      'city': 'Campinas',
                      'latitude': -22.8315,
                      'longitude': -47.0512,
                    },
                    'destination': {
                      'address': 'Centro',
                      'city': 'Campinas',
                      'latitude': -22.9000,
                      'longitude': -47.0600,
                    },
                    'departureTime': '08:00',
                    'estimatedArrivalTime': '08:45',
                    'weekDays': ['MONDAY', 'FRIDAY'],
                    'encodedPolyline': '_p~iF~ps|U_ulLnnqC',
                  },
                  'error': null,
                  'timestamp': '2026-10-22T18:00:00Z',
                },
              ),
            );
          },
        ),
      );

      final route = await repository.createRoute(
        const CreateRouteRequest(
          type: AppRouteType.passenger,
          origin: RoutePlaceModel(
            address: 'PUC',
            city: 'Campinas',
            latitude: -22.8315,
            longitude: -47.0512,
          ),
          destination: RoutePlaceModel(
            address: 'Centro',
            city: 'Campinas',
            latitude: -22.9000,
            longitude: -47.0600,
          ),
          departureTime: '08:00',
          estimatedArrivalTime: '08:45',
          weekDays: {'MONDAY', 'FRIDAY'},
        ),
      );

      expect(route.id, 'route_999');
      expect(route.encodedPolyline, '_p~iF~ps|U_ulLnnqC');
    });
  });
}
