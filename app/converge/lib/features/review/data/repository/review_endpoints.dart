// Autor: Cristian Eduardo Fava - RA 25000636

abstract final class ReviewEndpoints {
  static const String reviews = '/reviews';
  static const String given = '/reviews/given';
  static String received(String userId) => '/reviews/received/$userId';
}
