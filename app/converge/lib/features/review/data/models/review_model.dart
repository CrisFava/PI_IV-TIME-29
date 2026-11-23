// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../auth/data/models/participation_role.dart';

/// Uma avaliação entre motorista e passageiro, conforme `ReviewResponseDTO`.
@immutable
///
/// A avaliação é imutável: não há edição nem exclusão.
class ReviewModel {
  /// Id da avaliação.
  final String id;

  /// Viagem avaliada.
  final String rideId;

  /// Quem avaliou.
  final String reviewerId;

  /// Quem foi avaliado.
  final String reviewedId;

  /// Papel de quem foi avaliado na viagem.
  final ParticipationRole? reviewedRole;

  /// Nota de 1 a 5 estrelas.
  final int rating;

  /// Comentário, se houver.
  final String? comment;

  /// Quando a avaliação foi feita; nulo se a data não vier ou não puder ser lida.
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

  /// Lê a avaliação do JSON da API. Campos ausentes viram valores vazios em vez de erro.
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
