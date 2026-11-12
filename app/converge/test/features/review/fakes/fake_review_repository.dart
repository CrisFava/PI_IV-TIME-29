// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/auth/data/models/participation_role.dart';
import 'package:converge/features/review/data/models/create_review_request.dart';
import 'package:converge/features/review/data/models/review_model.dart';
import 'package:converge/features/review/data/repository/review_repository.dart';

/// Avaliações em memória. Recusa avaliar a mesma pessoa duas vezes na mesma viagem, como o backend.
class FakeReviewRepository implements ReviewRepository {
  final String reviewerId;
  final List<ReviewModel> reviews = [];

  FakeReviewRepository({required this.reviewerId});

  @override
  Future<ReviewModel> createReview(CreateReviewRequest request) async {
    final duplicated = reviews.any(
      (review) =>
          review.rideId == request.rideId &&
          review.reviewedId == request.reviewedId,
    );
    if (duplicated) {
      throw const ApiException(
        code: ApiErrorCode.unknown,
        message: 'Você já avaliou esse participante nesta viagem.',
        status: 409,
      );
    }

    final review = ReviewModel(
      id: 'review-${reviews.length + 1}',
      rideId: request.rideId,
      reviewerId: reviewerId,
      reviewedId: request.reviewedId,
      reviewedRole: null,
      rating: request.rating,
      comment: request.comment,
      createdAt: DateTime(2026, 11, 16, 9),
    );
    reviews.add(review);
    return review;
  }

  @override
  Future<List<ReviewModel>> listGivenInRide(String rideId) async => [
    for (final review in reviews)
      if (review.rideId == rideId) review,
  ];

  @override
  Future<List<ReviewModel>> listReceived(
    String userId, {
    required ParticipationRole role,
  }) async => const [];
}
