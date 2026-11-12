// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

/// Corpo do `POST /reviews`. Quem avalia é sempre o usuário logado.
@immutable
class CreateReviewRequest {
  final String rideId;
  final String reviewedId;

  /// Nota de 1 a 5.
  final int rating;
  final String? comment;

  const CreateReviewRequest({
    required this.rideId,
    required this.reviewedId,
    required this.rating,
    this.comment,
  });

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
