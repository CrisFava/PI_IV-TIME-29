// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

abstract final class MatchingEndpoints {
  /// `GET /matchings?routeId=`: o tipo do trajeto decide o lado da busca
  /// (procura lista motoristas; oferta lista passageiros).
  static const String matchings = '/matchings';
}
