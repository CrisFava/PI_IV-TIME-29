// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../data/models/ride_history_filter.dart';
import '../../state/ride_history_provider.dart';
import 'ride_history_filter_bar.dart';
import 'ride_history_message.dart';
import 'ride_history_tile.dart';

/// Histórico de viagens terminadas, da mais recente para a mais antiga (RF59),
/// com filtro de papel e de período. Puxe para baixo para atualizar.
class RideHistoryList extends ConsumerStatefulWidget {
  final double bottomPadding;

  const RideHistoryList({super.key, this.bottomPadding = 0});

  @override
  ConsumerState<RideHistoryList> createState() => _RideHistoryListState();
}

class _RideHistoryListState extends ConsumerState<RideHistoryList> {
  RideHistoryFilter _filter = const RideHistoryFilter();

  @override
  Widget build(BuildContext context) {
    final history = ref.watch(rideHistoryProvider(_filter));

    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () => ref.refresh(rideHistoryProvider(_filter).future),
      child: ListView(
        padding: EdgeInsets.fromLTRB(16, 16, 16, 120 + widget.bottomPadding),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          RideHistoryFilterBar(
            filter: _filter,
            onChanged: (filter) => setState(() => _filter = filter),
          ),
          const SizedBox(height: 14),
          ...switch (history) {
            AsyncData(:final value) when value.isEmpty => const [
              RideHistoryMessage(
                icon: Icons.history_rounded,
                text: 'Nenhuma viagem terminada por aqui. Elas aparecem quando uma carona é concluída ou cancelada.',
              ),
            ],
            AsyncData(:final value) => [
              Text(
                value.length == 1 ? '1 viagem' : '${value.length} viagens',
                style: const TextStyle(
                  fontSize: 12.5,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 8),
              for (final item in value)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: RideHistoryTile(item: item),
                ),
            ],
            AsyncError(:final error) => [
              RideHistoryMessage(
                icon: Icons.cloud_off_rounded,
                text: authErrorMessage(error),
              ),
            ],
            _ => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(
                  child: CircularProgressIndicator(color: AppColors.primary),
                ),
              ),
            ],
          },
        ],
      ),
    );
  }
}
