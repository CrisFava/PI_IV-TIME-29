// Autor: Allan Giovanni Matias Paes - RA 25008211

/// Endpoints do módulo de rotas, estimativas e pesquisa de locais (`/api/v1/places` e `/api/v1/routes`).
abstract final class RouteEndpoints {
  static const String placesAutocomplete = '/places/autocomplete';
  static String placeDetails(String placeId) => '/places/$placeId';
  static const String routes = '/routes';
  static const String routeEstimate = '/routes/estimate';
  static String routeById(String id) => '/routes/$id';
}
