// Autor: Allan Giovanni Matias Paes - RA 25008211

abstract final class RouteEndpoints {
  static const String placesAutocomplete = '/places/autocomplete';
  static String placeDetails(String placeId) => '/places/$placeId';
  static const String routes = '/routes';
  static String routeById(String id) => '/routes/$id';
}
