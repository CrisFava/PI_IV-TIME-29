// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../data/models/indicator_period.dart';
import '../../data/models/user_indicator_model.dart';
import '../../state/indicator_providers.dart';
import '../widgets/co2_highlight_card.dart';
import '../widgets/indicator_format.dart';
import '../widgets/indicator_period_selector.dart';
import '../widgets/indicator_stat_card.dart';
import '../widgets/methodology_sheet.dart';
import '../widgets/platform_impact_card.dart';

/// Indicadores de mobilidade e ambientais do usuário (RF51 a RF61). A mesma
/// tela para motorista e passageiro, com o período escolhido no topo.
class IndicatorsPage extends ConsumerStatefulWidget {
  const IndicatorsPage({super.key});

  @override
  ConsumerState<IndicatorsPage> createState() => _IndicatorsPageState();
}

class _IndicatorsPageState extends ConsumerState<IndicatorsPage> {
  bool _showEnvironmental = false;
  IndicatorPeriod _period = IndicatorPeriod.monthly;

  @override
  Widget build(BuildContext context) {
    final indicators = ref.watch(myIndicatorsProvider(_period));

    return Scaffold(
      backgroundColor: AppColors.scaffoldBackground,
      appBar: AppBar(
        backgroundColor: AppColors.cardBackground,
        surfaceTintColor: AppColors.cardBackground,
        foregroundColor: AppColors.textPrimary,
        title: const Text(
          'Indicadores',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
        ),
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        onRefresh: () async {
          ref.invalidate(myIndicatorsProvider(_period));
          ref.invalidate(platformIndicatorsProvider(_period));
          await ref.read(myIndicatorsProvider(_period).future);
        },
        child: ListView(
          padding: EdgeInsets.fromLTRB(
            16,
            16,
            16,
            24 + MediaQuery.paddingOf(context).bottom,
          ),
          children: [
            Row(
              children: [
                Expanded(
                  child: _tabButton(
                    'Mobilidade',
                    Icons.directions_car_rounded,
                    environmental: false,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _tabButton(
                    'Ambiental',
                    Icons.eco_rounded,
                    environmental: true,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            IndicatorPeriodSelector(
              selected: _period,
              onChanged: (period) => setState(() => _period = period),
            ),
            const SizedBox(height: 16),
            ...switch (indicators) {
              AsyncData(:final value) =>
                _showEnvironmental ? _environmental(value) : _mobility(value),
              AsyncError(:final error) => [
                Text(
                  authErrorMessage(error),
                  style: const TextStyle(
                    fontSize: 13,
                    color: AppColors.errorText,
                  ),
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
      ),
    );
  }

  Widget _tabButton(
    String label,
    IconData icon, {
    required bool environmental,
  }) {
    final selected = _showEnvironmental == environmental;
    return Material(
      color: selected ? AppColors.primary : AppColors.cardBackground,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        key: ValueKey('indicators_tab_$label'),
        borderRadius: BorderRadius.circular(14),
        onTap: () {
          if (selected) return;
          HapticFeedback.lightImpact();
          setState(() => _showEnvironmental = environmental);
        },
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 13),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.primary),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 18,
                color: selected
                    ? AppColors.dockItemActive
                    : AppColors.textPrimary,
              ),
              const SizedBox(width: 6),
              Text(
                label,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: selected
                      ? AppColors.dockItemActive
                      : AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  List<Widget> _mobility(UserIndicatorModel value) => [
    IndicatorStatCard(
      icon: Icons.directions_car_rounded,
      value: formatIndicatorInt(value.totalRides),
      label: value.totalRides == 1 ? 'carona' : 'caronas',
      detail:
          '${formatIndicatorInt(value.ridesAsDriver)} como motorista, '
          '${formatIndicatorInt(value.ridesAsPassenger)} como passageiro',
    ),
    const SizedBox(height: 10),
    Row(
      children: [
        Expanded(
          child: IndicatorStatCard(
            icon: Icons.route_rounded,
            value: '${formatIndicatorDecimal(value.sharedKm)} km',
            label: 'compartilhados',
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: IndicatorStatCard(
            icon: Icons.no_crash_rounded,
            value: formatIndicatorInt(value.carsAvoided),
            label: 'carros evitados',
          ),
        ),
      ],
    ),
    const SizedBox(height: 10),
    PlatformImpactCard(period: _period),
  ];

  List<Widget> _environmental(UserIndicatorModel value) => [
    Co2HighlightCard(
      co2AvoidedKg: value.co2AvoidedKg,
      equivalentTrees: value.equivalentTrees,
    ),
    const SizedBox(height: 10),
    Row(
      children: [
        Expanded(
          child: IndicatorStatCard(
            icon: Icons.park_rounded,
            value: formatIndicatorDecimal(value.equivalentTrees),
            label: 'árvores em um ano',
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: IndicatorStatCard(
            icon: Icons.route_rounded,
            value: '${formatIndicatorDecimal(value.sharedKm)} km',
            label: 'compartilhados',
          ),
        ),
      ],
    ),
    const SizedBox(height: 6),
    TextButton.icon(
      key: const ValueKey('how_we_calculate_button'),
      onPressed: () => showMethodologySheet(context),
      style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
      icon: const Icon(Icons.info_outline_rounded, size: 18),
      label: const Text(
        'Como calculamos?',
        style: TextStyle(fontWeight: FontWeight.w700),
      ),
    ),
  ];
}
