// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../auth/data/models/participation_role.dart';

/// Uma avaliação entre motorista e passageiro, conforme `ReviewResponseDTO`.
@immutable
class ReviewModel {
  final String id;
  final String rideId;
  final String reviewerId;
  final String reviewedId;

  /// Papel de quem foi avaliado na viagem.
  final ParticipationRole? reviewedRole;
  final int rating;
  final String? comment;
  final DateTime? createdAt;

  const ReviewModel({
    required this.id,
    required this.rideId,
    required this.reviewerId,
    required this.reviewedId,
    required this.reviewedRole,
    required this.rating,
    required this.comment,
    required this.createdAt,
  });

  factory ReviewModel.fromJson(Map<String, dynamic> json) => ReviewModel(
    id: json['id'] as String? ?? '',
    rideId: json['rideId'] as String? ?? '',
    reviewerId: json['reviewerId'] as String? ?? '',
    reviewedId: json['reviewedId'] as String? ?? '',
    reviewedRole: ParticipationRole.fromJson(json['reviewedRole'] as String?),
    rating: (json['rating'] as num?)?.toInt() ?? 0,
    comment: json['comment'] as String?,
    createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
  );
}
