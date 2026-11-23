// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

/// Corpo do `POST /reviews`. Quem avalia é sempre o usuário logado.
@immutable
///
/// O papel do avaliado (`reviewedRole`) não é enviado: o servidor calcula pela viagem.
class CreateReviewRequest {
  /// Viagem concluída em que os dois participaram.
  final String rideId;

  /// Id de quem é avaliado (o motorista ou um passageiro).
  final String reviewedId;

  /// Nota de 1 a 5.
  final int rating;

  /// Comentário opcional.
  final String? comment;

  const CreateReviewRequest({
    required this.rideId,
    required this.reviewedId,
    required this.rating,
    this.comment,
  });

  /// Corpo da requisição. Comentário vazio ou só com espaços não é enviado.
  Map<String, dynamic> toJson() {
    final trimmed = comment?.trim() ?? '';
    return {
      'rideId': rideId,
      'reviewedId': reviewedId,
      'rating': rating,
      if (trimmed.isNotEmpty) 'comment': trimmed,
    };
  }
}
