// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/session/auth_session.dart';
import '../../data/models/ride_model.dart';
import '../../state/ride_providers.dart';
import 'ride_history_message.dart';
import 'ride_history_tile.dart';

/// Histórico de viagens do usuário, da mais recente para a mais antiga,
/// com a etiqueta do papel dele em cada uma. Puxe para baixo para atualizar.
class RideHistoryList extends ConsumerWidget {
  final double bottomPadding;

  const RideHistoryList({super.key, this.bottomPadding = 0});

  static List<RideModel> _newestFirst(List<RideModel> rides) {
    final sorted = [...rides];
    sorted.sort((a, b) {
      final aDate = a.departureDateTime;
      final bDate = b.departureDateTime;
      if (aDate == null && bDate == null) return 0;
      if (aDate == null) return 1;
      if (bDate == null) return -1;
      return bDate.compareTo(aDate);
    });
    return sorted;
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(authControllerProvider).value;
    final userId = session is Authenticated ? session.user.id : null;
    final rides = ref.watch(myRidesProvider);
    final padding = EdgeInsets.fromLTRB(16, 16, 16, 120 + bottomPadding);

    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () => ref.refresh(myRidesProvider.future),
      child: switch (rides) {
        AsyncData(:final value) when value.isEmpty => ListView(
          padding: padding,
          physics: const AlwaysScrollableScrollPhysics(),
          children: const [
            RideHistoryMessage(
              icon: Icons.history_rounded,
              text: 'Você ainda não tem viagens. Elas aparecem aqui quando você pede uma vaga ou oferece uma carona.',
            ),
          ],
        ),
        AsyncData(:final value) => ListView(
          padding: padding,
          physics: const AlwaysScrollableScrollPhysics(),
          children: [
            for (final ride in _newestFirst(value))
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: RideHistoryTile(
                  ride: ride,
                  isDriver: userId != null && ride.isDrivenBy(userId),
                ),
              ),
          ],
        ),
        AsyncError(:final error) => ListView(
          padding: padding,
          physics: const AlwaysScrollableScrollPhysics(),
          children: [
            RideHistoryMessage(
              icon: Icons.cloud_off_rounded,
              text: authErrorMessage(error),
            ),
          ],
        ),
        _ => const Center(
          child: CircularProgressIndicator(color: AppColors.primary),
        ),
      },
    );
  }
}
