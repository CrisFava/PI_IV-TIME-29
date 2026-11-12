// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/create_review_request.dart';
import '../data/models/review_model.dart';
import '../data/repository/review_repository.dart';
import '../data/repository/review_repository_impl.dart';

/// Avaliações que o usuário logado já fez em uma viagem (chave: id da viagem).
final rideReviewsProvider = AsyncNotifierProvider.autoDispose
    .family<RideReviewsController, List<ReviewModel>, String>(
      RideReviewsController.new,
    );

class RideReviewsController extends AsyncNotifier<List<ReviewModel>> {
  RideReviewsController(this.rideId);

  final String rideId;

  ReviewRepository get _repository => ref.read(reviewRepositoryProvider);

  @override
  Future<List<ReviewModel>> build() => _repository.listGivenInRide(rideId);

  /// Envia a avaliação e já a inclui na lista, para a tela esconder o botão "Avaliar".
  /// Erros são propagados para a tela exibir a mensagem.
  Future<ReviewModel> submit({
    required String reviewedId,
    required int rating,
    String? comment,
  }) async {
    final review = await _repository.createReview(
      CreateReviewRequest(
        rideId: rideId,
        reviewedId: reviewedId,
        rating: rating,
        comment: comment,
      ),
    );

    if (ref.mounted) {
      state = AsyncData([...state.value ?? const <ReviewModel>[], review]);
    }
    return review;
  }
}
