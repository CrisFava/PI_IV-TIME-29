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

/// Carrega e mantém as avaliações que o usuário logado já fez em uma viagem.
///
/// As telas usam a lista para saber quem ainda falta avaliar e esconder o botão "Avaliar" depois do
/// envio, sem buscar de novo na API.
class RideReviewsController extends AsyncNotifier<List<ReviewModel>> {
  RideReviewsController(this.rideId);

  /// Viagem das avaliações.
  final String rideId;

  /// Chamadas de avaliação na API.
  ReviewRepository get _repository => ref.read(reviewRepositoryProvider);

  /// Busca as avaliações feitas na viagem (`GET /reviews/given`).
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
