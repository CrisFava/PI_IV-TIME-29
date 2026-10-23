import '../models/create_route_request.dart';
import '../models/place_details_model.dart';
import '../models/place_suggestion_model.dart';
import '../models/route_response_model.dart';

abstract class RouteRepository {
  /// Busca sugestões de endereço no backend via Google Places.
  Future<List<PlaceSuggestionModel>> autocomplete({
    required String input,
    String? sessionToken,
  });

  /// Busca os detalhes e coordenadas de um lugar pelo seu [placeId].
  Future<PlaceDetailsModel> getPlaceDetails({
    required String placeId,
    String? sessionToken,
  });

  /// Salva um novo trajeto (oferta ou pedido de carona) no backend.
  Future<RouteResponseModel> createRoute(CreateRouteRequest request);

  /// Lista os trajetos ativos/salvos do usuário autenticado.
  Future<List<RouteResponseModel>> listMyRoutes();

  /// Busca os dados de um trajeto específico pelo seu [routeId].
  Future<RouteResponseModel> getRoute(String routeId);

  /// Remove um trajeto pelo seu [routeId].
  Future<void> deleteRoute(String routeId);
}
