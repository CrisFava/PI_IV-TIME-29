// Autor: Cristian Eduardo Fava - RA 25000636

import '../../../auth/data/models/participation_role.dart';
import '../models/create_review_request.dart';
import '../models/review_model.dart';

/// Contrato com a API de avaliações (`/reviews`).
abstract class ReviewRepository {
  /// Avalia outro participante de uma viagem concluída.
  Future<ReviewModel> createReview(CreateReviewRequest request);

  /// Avaliações que o usuário logado já fez em uma viagem.
  Future<List<ReviewModel>> listGivenInRide(String rideId);

  /// Avaliações recebidas por um usuário no papel informado.
  Future<List<ReviewModel>> listReceived(
    String userId, {
    required ParticipationRole role,
  });
}
