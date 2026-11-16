// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/ride_history_filter.dart';
import '../data/models/ride_history_item.dart';
import '../data/repository/ride_history_repository.dart';

/// Viagens terminadas do usuário com os filtros escolhidos (RF59).
final rideHistoryProvider = FutureProvider.autoDispose
    .family<List<RideHistoryItem>, RideHistoryFilter>(
      (ref, filter) =>
          ref.watch(rideHistoryRepositoryProvider).list(filter, DateTime.now()),
    );
