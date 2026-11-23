// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/ride_model.dart';
import '../../ride/state/ride_providers.dart';
import 'matching_viewer_provider.dart';
import 'ride_cancellation_notices.dart';

/// Avisos de cancelamento ainda não vistos, do mais antigo ao mais novo: o
/// motorista é avisado quando um passageiro sai ou desiste do pedido, e o
/// passageiro, quando o motorista cancela a carona, o tira dela ou não aceita o
/// pedido. Só existe enquanto alguém o observa (a tela inicial, o tempo todo).
final rideCancellationNoticeProvider =
    NotifierProvider.autoDispose<
      RideCancellationNoticeController,
      List<RideCancellationNotice>
    >(RideCancellationNoticeController.new);

class RideCancellationNoticeController
    extends Notifier<List<RideCancellationNotice>> {
  // A última leitura das viagens, para comparar com a próxima
  List<RideModel>? _last;
  final Set<String> _shown = {};

  @override
  List<RideCancellationNotice> build() {
    final viewerId = ref.watch(matchingViewerIdProvider);
    if (viewerId == null) return const [];

    // As viagens são relidas de tempos em tempos: o cancelamento do outro lado
    // chega sem o usuário precisar abrir nada
    final timer = Timer.periodic(
      ref.read(matchingPollIntervalProvider),
      (_) => ref.invalidate(myRidesProvider),
    );
    ref.onDispose(timer.cancel);

    _last = ref.read(myRidesProvider).value;
    ref.listen(myRidesProvider, (_, next) {
      final rides = next.value;
      if (rides == null) return;
      final before = _last;
      _last = rides;
      // A primeira leitura só serve de base: nada mudou ainda
      if (before == null) return;
      final fresh = [
        for (final notice in rideCancellationNotices(
          before: before,
          after: rides,
          viewerId: viewerId,
        ))
          if (_shown.add(notice.key)) notice,
      ];
      if (fresh.isEmpty) return;
      Future.microtask(() {
        if (ref.mounted) state = [...state, ...fresh];
      });
    });
    return const [];
  }

  /// O aviso foi visto ou fechado: sai da fila.
  void dismiss(String key) {
    state = [
      for (final notice in state)
        if (notice.key != key) notice,
    ];
  }
}
