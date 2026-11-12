// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/review/data/models/create_review_request.dart';
import 'package:converge/features/review/data/models/review_model.dart';
import 'package:converge/features/review/data/repository/review_repository_impl.dart';
import 'package:converge/features/review/state/ride_reviews_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_review_repository.dart';

void main() {
  late FakeReviewRepository reviews;
  late ProviderContainer container;

  setUp(() {
    reviews = FakeReviewRepository(reviewerId: 'passenger-1');
    container = ProviderContainer.test(
      overrides: [reviewRepositoryProvider.overrideWithValue(reviews)],
    );
    container.listen(rideReviewsProvider('ride-1'), (_, _) {});
  });

  test('começa com as avaliações que o usuário já fez na viagem', () async {
    await reviews.createReview(
      const CreateReviewRequest(rideId: 'ride-1', reviewedId: 'driver-1', rating: 4),
    );
    await reviews.createReview(
      const CreateReviewRequest(rideId: 'ride-2', reviewedId: 'driver-9', rating: 2),
    );

    // O setUp já carregou a lista vazia; recarrega para enxergar as avaliações criadas.
    container.invalidate(rideReviewsProvider('ride-1'));
    final given = await container.read(rideReviewsProvider('ride-1').future);

    expect(given.map((review) => review.reviewedId), ['driver-1']);
  });

  test('enviar inclui a avaliação na lista sem recarregar', () async {
    await container.read(rideReviewsProvider('ride-1').future);

    final review = await container
        .read(rideReviewsProvider('ride-1').notifier)
        .submit(reviewedId: 'driver-1', rating: 5, comment: 'Ótima carona');

    expect(review.rating, 5);
    final given = container.read(rideReviewsProvider('ride-1')).value!;
    expect(given.single.reviewedId, 'driver-1');
    expect(given.single.comment, 'Ótima carona');
  });

  test('avaliar a mesma pessoa de novo propaga o erro do backend', () async {
    await container.read(rideReviewsProvider('ride-1').future);
    final controller = container.read(rideReviewsProvider('ride-1').notifier);
    await controller.submit(reviewedId: 'driver-1', rating: 5);

    await expectLater(
      controller.submit(reviewedId: 'driver-1', rating: 1),
      throwsA(isA<ApiException>().having((e) => e.status, 'status', 409)),
    );
    expect(container.read(rideReviewsProvider('ride-1')).value, hasLength(1));
  });

  group('modelos', () {
    test('CreateReviewRequest omite comentário em branco', () {
      const blank = CreateReviewRequest(
        rideId: 'ride-1',
        reviewedId: 'driver-1',
        rating: 5,
        comment: '   ',
      );
      const filled = CreateReviewRequest(
        rideId: 'ride-1',
        reviewedId: 'driver-1',
        rating: 5,
        comment: ' Pontual ',
      );

      expect(blank.toJson(), {
        'rideId': 'ride-1',
        'reviewedId': 'driver-1',
        'rating': 5,
      });
      expect(filled.toJson()['comment'], 'Pontual');
    });

    test('ReviewModel lê a resposta do backend', () {
      final review = ReviewModel.fromJson(const {
        'id': 'review-1',
        'rideId': 'ride-1',
        'reviewerId': 'passenger-1',
        'reviewedId': 'driver-1',
        'reviewedRole': 'DRIVER',
        'rating': 5,
        'comment': 'Ótima carona',
        'createdAt': '2026-11-16T12:00:00Z',
      });

      expect(review.id, 'review-1');
      expect(review.rating, 5);
      expect(review.reviewedRole?.toJson(), 'DRIVER');
      expect(review.createdAt, DateTime.utc(2026, 11, 16, 12));
    });
  });
}
