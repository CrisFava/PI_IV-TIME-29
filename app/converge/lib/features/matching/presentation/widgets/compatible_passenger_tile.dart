// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/match_model.dart';
import 'match_info_chip.dart';

/// Um passageiro compatível com o trajeto do motorista (ele ainda não pediu vaga).
class CompatiblePassengerTile extends StatelessWidget {
  final MatchModel match;

  const CompatiblePassengerTile({super.key, required this.match});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.cardSecondary,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  match.otherUserName,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              Text(
                match.compatibilityLabel,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w800,
                  color: AppColors.success,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '${match.origin.address} → ${match.destination.address}',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontSize: 12,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              MatchInfoChip(
                icon: Icons.schedule_rounded,
                label: 'Sai às ${match.departureTime}',
              ),
              MatchInfoChip(
                icon: Icons.alt_route_rounded,
                label: '+${match.calculatedDetourKm.toStringAsFixed(1)} km',
              ),
            ],
          ),
        ],
      ),
    );
  }
}
