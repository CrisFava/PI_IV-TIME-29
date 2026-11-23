// Autor: Cristian Eduardo Fava - RA 25000636

/// Endpoints do módulo de avaliações entre usuários (`/api/v1/reviews`).
abstract final class ReviewEndpoints {
  static const String reviews = '/reviews';
  static const String given = '/reviews/given';
  static String received(String userId) => '/reviews/received/$userId';
}
