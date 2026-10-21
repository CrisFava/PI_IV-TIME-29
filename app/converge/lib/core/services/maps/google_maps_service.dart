import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

/// ============================================================================
/// EXCEÇÃO TIPADA: RoutesException
/// ============================================================================
/// Lançada em caso de erro na consulta, validação ou decodificação de rotas
/// da Google Routes API.
/// ============================================================================
class RoutesException implements Exception {
  /// Mensagem descritiva e amigável sobre o erro ocorrido.
  final String message;

  /// Status de erro retornado pela Google Routes API (ex: PERMISSION_DENIED, RESOURCE_EXHAUSTED, INVALID_ARGUMENT, ZERO_RESULTS).
  final String? apiStatus;

  /// Mensagem técnica detalhada (`error.message`) retornada pelo Google, se disponível.
  final String? apiErrorMessage;

  const RoutesException(
    this.message, {
    this.apiStatus,
    this.apiErrorMessage,
  });

  @override
  String toString() {
    final buffer = StringBuffer('RoutesException: $message');
    if (apiStatus != null) {
      buffer.write(' [status: $apiStatus]');
    }
    if (apiErrorMessage != null && apiErrorMessage!.isNotEmpty) {
      buffer.write(' [details: $apiErrorMessage]');
    }
    return buffer.toString();
  }
}

/// Compatibilidade retroativa para callers que utilizavam DirectionsException
typedef DirectionsException = RoutesException;

/// ============================================================================
/// SERVIÇO DE INTEGRAÇÃO COM GOOGLE MAPS API (ROUTES API & GEOCODING)
/// ============================================================================
/// Responsável por:
/// 1. Consultar a moderna Google Routes API (v2:computeRoutes) em modo 'DRIVE' para traçar rotas viárias reais.
/// 2. Validar a integridade da resposta, headers obrigatórios e status RPC de erro.
/// 3. Decodificar o encodedPolyline retornado.
/// 4. Geocodificação reversa de coordenadas para exibição de endereços reais.
/// ============================================================================
class GoogleMapsService {
  @visibleForTesting
  static Dio? testDio;

  static final Dio _defaultDio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 8),
      receiveTimeout: const Duration(seconds: 8),
    ),
  );

  static Dio get _dio => testDio ?? _defaultDio;

  static const String apiKey = String.fromEnvironment(
    'GOOGLE_MAPS_API_KEY'
  );

  /// Busca a rota viária de carro seguindo as ruas da cidade via Google Routes API (v2).
  ///
  /// Lança [RoutesException] se:
  /// - A API retornar erro RPC (ex: PERMISSION_DENIED, RESOURCE_EXHAUSTED, INVALID_ARGUMENT).
  /// - Nenhuma rota viária for encontrada para as coordenadas informadas.
  /// - A resposta for inválida ou não contiver um encodedPolyline válido.
  /// - Ocorrer falha de rede, timeout ou erro na decodificação das coordenadas.
  ///
  /// Nunca retorna coordenadas artificiais ou linhas retas fictícias.
  static Future<List<LatLng>> computeRoutes({
    required LatLng origin,
    required LatLng destination,
  }) async {
    Response<Map<String, dynamic>> response;
    try {
      response = await _dio.post<Map<String, dynamic>>(
        'https://routes.googleapis.com/directions/v2:computeRoutes',
        options: Options(
          headers: {
            'Content-Type': 'application/json',
            'X-Goog-Api-Key': apiKey,
            'X-Goog-FieldMask': 'routes.polyline.encodedPolyline',
          },
        ),
        data: {
          'origin': {
            'location': {
              'latLng': {
                'latitude': origin.latitude,
                'longitude': origin.longitude,
              },
            },
          },
          'destination': {
            'location': {
              'latLng': {
                'latitude': destination.latitude,
                'longitude': destination.longitude,
              },
            },
          },
          'travelMode': 'DRIVE',
          'routingPreference': 'TRAFFIC_UNAWARE',
        },
      );
    } on DioException catch (e) {
      if (e.type == DioExceptionType.badResponse && e.response?.data != null) {
        final errorData = e.response!.data;
        if (errorData is Map<String, dynamic> &&
            errorData['error'] is Map<String, dynamic>) {
          final err = errorData['error'] as Map<String, dynamic>;
          final status = err['status'] as String?;
          final detailsMsg = err['message'] as String?;

          final friendly = switch (status) {
            'PERMISSION_DENIED' =>
              'Acesso negado à Routes API. Verifique a chave de API e se a Routes API está ativada no Google Cloud Console.',
            'RESOURCE_EXHAUSTED' =>
              'Limite de requisições da Routes API excedido temporariamente.',
            'INVALID_ARGUMENT' =>
              'Parâmetros inválidos enviados para a Routes API.',
            'NOT_FOUND' =>
              'Origem ou destino não puderam ser localizados em vias trafegáveis.',
            _ =>
              'Erro retornado pela Routes API: ${status ?? detailsMsg ?? "desconhecido"}.',
          };

          throw RoutesException(
            friendly,
            apiStatus: status,
            apiErrorMessage: detailsMsg,
          );
        }
      }

      final message = switch (e.type) {
        DioExceptionType.connectionTimeout ||
        DioExceptionType.sendTimeout ||
        DioExceptionType.receiveTimeout =>
          'Tempo limite esgotado ao conectar à Google Routes API.',
        DioExceptionType.connectionError =>
          'Sem conexão com o serviço de rotas. Verifique sua conexão com a internet.',
        DioExceptionType.badResponse =>
          'Erro HTTP ${e.response?.statusCode ?? 0} ao consultar rotas.',
        _ => 'Falha de rede ao consultar rotas: ${e.message ?? "erro desconhecido"}',
      };
      throw RoutesException(message);
    } catch (e) {
      if (e is RoutesException) rethrow;
      throw RoutesException('Erro inesperado ao consultar a rota: $e');
    }

    final data = response.data;
    if (data == null) {
      throw const RoutesException('Resposta vazia da Google Routes API.');
    }

    final routes = data['routes'] as List<dynamic>?;
    if (routes == null || routes.isEmpty) {
      throw const RoutesException(
        'Nenhuma rota viária encontrada entre os pontos selecionados.',
        apiStatus: 'ZERO_RESULTS',
      );
    }

    final firstRoute = routes[0];
    if (firstRoute is! Map<String, dynamic>) {
      throw const RoutesException('Formato de rota inválido na resposta da API.');
    }

    final polylineObj = firstRoute['polyline'];
    if (polylineObj is! Map<String, dynamic>) {
      throw const RoutesException('Objeto polyline ausente na rota retornada.');
    }

    final encodedPolyline = polylineObj['encodedPolyline'] as String?;
    if (encodedPolyline == null || encodedPolyline.isEmpty) {
      throw const RoutesException(
        'A rota retornada não possui pontos de traçado (encodedPolyline vazio).',
      );
    }

    List<LatLng> decodedPoints;
    try {
      decodedPoints = decodePolyline(encodedPolyline);
    } catch (e) {
      throw RoutesException('Falha ao decodificar a polyline da rota: $e');
    }

    if (decodedPoints.isEmpty) {
      throw const RoutesException('A rota decodificada não possui coordenadas válidas.');
    }

    return decodedPoints;
  }

  /// Delega para [computeRoutes] para compatibilidade com o chamador anterior.
  static Future<List<LatLng>> getDirectionsRoute({
    required LatLng origin,
    required LatLng destination,
  }) =>
      computeRoutes(origin: origin, destination: destination);

  /// Geocodificação reversa para buscar nome de rua/endereço a partir de coordenadas.
  /// Isolada e independente da lógica de roteamento viário.
  static Future<String> getAddressFromCoordinates(LatLng position) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        'https://maps.googleapis.com/maps/api/geocode/json',
        queryParameters: {
          'latlng': '${position.latitude},${position.longitude}',
          'key': apiKey,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data!;
        final status = data['status'] as String?;
        if (status == 'OK') {
          final results = data['results'] as List<dynamic>?;
          if (results != null && results.isNotEmpty) {
            final formatted = results[0]['formatted_address'] as String?;
            if (formatted != null && formatted.isNotEmpty) {
              final parts = formatted.split(',');
              if (parts.length >= 2) {
                return '${parts[0].trim()}, ${parts[1].trim()}';
              }
              return formatted;
            }
          }
        }
      }
    } catch (e) {
      debugPrint('Google Geocoding API: $e');
    }

    return 'Ponto selecionado (${position.latitude.toStringAsFixed(4)}, ${position.longitude.toStringAsFixed(4)})';
  }

  /// Algoritmo oficial de decodificação de polylines do Google Maps (encodedPolyline).
  static List<LatLng> decodePolyline(String encoded) {
    final poly = <LatLng>[];
    int index = 0;
    final len = encoded.length;
    int lat = 0;
    int lng = 0;

    while (index < len) {
      int b;
      int shift = 0;
      int result = 0;
      do {
        if (index >= len) {
          throw const FormatException('String polyline truncada ou malformada.');
        }
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      final dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lat += dlat;

      shift = 0;
      result = 0;
      do {
        if (index >= len) {
          throw const FormatException('String polyline truncada ou malformada.');
        }
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      final dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lng += dlng;

      poly.add(LatLng(lat / 1e5, lng / 1e5));
    }
    return poly;
  }
}
