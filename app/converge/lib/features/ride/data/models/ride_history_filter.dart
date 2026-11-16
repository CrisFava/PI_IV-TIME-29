// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../../auth/data/models/participation_role.dart';

/// Filtros do histórico: o papel na viagem e quantos dias para trás (null = tudo).
@immutable
class RideHistoryFilter {
  final ParticipationRole? role;
  final int? lastDays;

  const RideHistoryFilter({this.role, this.lastDays});

  RideHistoryFilter copyWith({
    Object? role = _unset,
    Object? lastDays = _unset,
  }) => RideHistoryFilter(
    role: identical(role, _unset) ? this.role : role as ParticipationRole?,
    lastDays: identical(lastDays, _unset) ? this.lastDays : lastDays as int?,
  );

  static const Object _unset = Object();

  @override
  bool operator ==(Object other) =>
      other is RideHistoryFilter &&
      other.role == role &&
      other.lastDays == lastDays;

  @override
  int get hashCode => Object.hash(role, lastDays);
}
