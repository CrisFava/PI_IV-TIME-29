// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/route/data/models/create_route_request.dart';
import 'package:converge/features/route/data/models/place_details_model.dart';
import 'package:converge/features/route/data/models/place_suggestion_model.dart';
import 'package:converge/features/route/data/models/route_response_model.dart';
import 'package:converge/features/route/data/repository/route_repository.dart';

/// Repositório de trajetos em memória, para o passageiro salvar o trajeto de procura.
class FakeMatchingRouteRepository implements RouteRepository {
  final List<RouteResponseModel> routes = [];

  /// Simula outro pedido salvando o mesmo trajeto no meio do caminho (409).
  bool conflictOnCreate = false;

  final List<String> calls = [];

  static RouteResponseModel routeFrom(String id, CreateRouteRequest request) =>
      RouteResponseModel.fromJson({
        'id': id,
        'type': request.type.apiValue,
        'status': 'ACTIVE',
        'origin': request.origin.toJson(),
        'destination': request.destination.toJson(),
        'departureTime': request.departureTime,
        'estimatedArrivalTime': request.estimatedArrivalTime,
        'weekDays': request.weekDays.toList(),
      });

  @override
  Future<RouteResponseModel> createRoute(CreateRouteRequest request) async {
    calls.add('createRoute');
    if (conflictOnCreate) {
      routes.add(routeFrom('saved-elsewhere', request));
      throw const ApiException(
        code: ApiErrorCode.unknown,
        message: 'Você já tem um trajeto igual a este.',
        status: 409,
      );
    }
    final route = routeFrom('created-${routes.length + 1}', request);
    routes.add(route);
    return route;
  }

  @override
  Future<List<RouteResponseModel>> listMyRoutes() async {
    calls.add('listMyRoutes');
    return List.of(routes);
  }

  @override
  Future<List<PlaceSuggestionModel>> autocomplete({
    required String input,
    String? sessionToken,
  }) => throw UnimplementedError();

  @override
  Future<PlaceDetailsModel> getPlaceDetails({
    required String placeId,
    String? sessionToken,
  }) => throw UnimplementedError();

  @override
  Future<RouteResponseModel> getRoute(String routeId) =>
      throw UnimplementedError();

  @override
  Future<void> deleteRoute(String routeId) => throw UnimplementedError();
}
