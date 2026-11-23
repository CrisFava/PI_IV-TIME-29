// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter/foundation.dart';

/// Um usuário bloqueado pelo usuário logado, conforme `BlockResponseDTO`.
@immutable
class BlockModel {
  final String id;
  final String blockedUserId;

  /// Só vem preenchido na lista de bloqueados.
  final String? blockedUserName;
  final DateTime? createdAt;

  const BlockModel({
    required this.id,
    required this.blockedUserId,
    required this.blockedUserName,
    required this.createdAt,
  });

  /// Nome para exibir; sem nome, um texto genérico.
  String get displayName {
    final value = blockedUserName?.trim() ?? '';
    return value.isNotEmpty ? value : 'Usuário';
  }

  factory BlockModel.fromJson(Map<String, dynamic> json) => BlockModel(
    id: json['id'] as String? ?? '',
    blockedUserId: json['blockedUserId'] as String? ?? '',
    blockedUserName: json['blockedUserName'] as String?,
    createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
  );
}
