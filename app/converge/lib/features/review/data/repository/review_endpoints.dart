// Autor: Cristian Eduardo Fava - RA 25000636

/// Endpoints do módulo de avaliações entre usuários (`/api/v1/reviews`).
abstract final class ReviewEndpoints {
  /// Cria uma avaliação (POST).
  static const String reviews = '/reviews';

  /// Avaliações feitas pelo usuário logado em uma viagem (GET `?rideId=`).
  static const String given = '/reviews/given';

  /// Avaliações recebidas por um usuário (GET `?role=`).
  static String received(String userId) => '/reviews/received/$userId';
}
