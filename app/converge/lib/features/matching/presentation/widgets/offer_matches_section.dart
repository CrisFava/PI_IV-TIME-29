// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../route/data/models/route_response_model.dart';
import '../../state/driver_matches_provider.dart';
import 'compatible_passenger_tile.dart';
import 'matching_message.dart';

/// Passageiros compatíveis com um trajeto de oferta.
class OfferMatchesSection extends ConsumerWidget {
  final RouteResponseModel route;

  const OfferMatchesSection({super.key, required this.route});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final matches = ref.watch(driverMatchesProvider(route.id));

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            '${route.origin.address} → ${route.destination.address} • ${route.departureTime}',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w700,
              color: AppColors.textDark,
            ),
          ),
          const SizedBox(height: 8),
          ...matches.when(
            data: (list) => list.isEmpty
                ? const [
                    MatchingMessage(
                      text: 'Nenhum passageiro compatível por enquanto.',
                    ),
                  ]
                : [
                    for (final match in list)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: CompatiblePassengerTile(match: match),
                      ),
                  ],
            loading: () => const [
              LinearProgressIndicator(
                color: AppColors.primary,
                backgroundColor: AppColors.progressTrack,
              ),
            ],
            error: (error, _) => [
              MatchingMessage(text: authErrorMessage(error)),
            ],
          ),
        ],
      ),
    );
  }
}
