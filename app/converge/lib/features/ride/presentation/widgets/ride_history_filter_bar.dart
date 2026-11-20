// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/data/models/participation_role.dart';
import '../../data/models/ride_history_filter.dart';

/// Filtros do histórico: o papel na viagem e o período.
class RideHistoryFilterBar extends StatelessWidget {
  final RideHistoryFilter filter;
  final ValueChanged<RideHistoryFilter> onChanged;

  const RideHistoryFilterBar({
    super.key,
    required this.filter,
    required this.onChanged,
  });

  static const _roles = <(String, ParticipationRole?)>[
    ('Todas', null),
    ('Motorista', ParticipationRole.driver),
    ('Passageiro', ParticipationRole.passenger),
  ];

  static const _periods = <(String, int?)>[
    ('30 dias', 30),
    ('6 meses', 182),
    ('Tudo', null),
  ];

  @override
  Widget build(BuildContext context) {
    // Uma linha para o papel e outra para o período: nenhum filtro fica sozinho
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 6,
          runSpacing: 6,
          children: [
            for (final (label, role) in _roles)
              _chip(
                label,
                filter.role == role,
                () => onChanged(filter.copyWith(role: role)),
              ),
          ],
        ),
        const SizedBox(height: 6),
        Wrap(
          spacing: 6,
          runSpacing: 6,
          children: [
            for (final (label, days) in _periods)
              _chip(
                label,
                filter.lastDays == days,
                () => onChanged(filter.copyWith(lastDays: days)),
              ),
          ],
        ),
      ],
    );
  }

  Widget _chip(String label, bool selected, VoidCallback onTap) {
    return ChoiceChip(
      key: ValueKey('history_filter_$label'),
      label: Text(label),
      selected: selected,
      showCheckmark: false,
      selectedColor: AppColors.primary,
      backgroundColor: AppColors.chipBackground,
      labelStyle: TextStyle(
        fontSize: 12,
        fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
        color: selected ? AppColors.dockItemActive : AppColors.textPrimary,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10),
        side: BorderSide(
          color: selected ? AppColors.primary : AppColors.chipBorder,
        ),
      ),
      onSelected: (isSelected) {
        if (!isSelected) return;
        HapticFeedback.selectionClick();
        onTap();
      },
    );
  }
}
