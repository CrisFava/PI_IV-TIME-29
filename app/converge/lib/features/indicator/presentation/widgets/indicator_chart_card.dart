// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/indicator_period.dart';
import '../../data/models/user_indicator_model.dart';
import '../../state/indicator_providers.dart';
import 'indicator_chart_painter.dart';
import 'indicator_series_label.dart';

/// Cartão com o gráfico das últimas semanas ou meses: barras ou linha.
class IndicatorChartCard extends ConsumerWidget {
  final String title;
  final IndicatorPeriod period;
  final bool asLine;
  final double Function(UserIndicatorModel point) valueOf;

  const IndicatorChartCard({
    super.key,
    required this.title,
    required this.period,
    required this.valueOf,
    this.asLine = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final series = ref.watch(myIndicatorSeriesProvider(period));

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 14),
          SizedBox(
            height: 140,
            child: switch (series) {
              AsyncData(:final value) => _chart(value),
              AsyncError() => const Center(
                child: Text(
                  'Não foi possível carregar o gráfico.',
                  style: TextStyle(
                    fontSize: 12.5,
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
              _ => const Center(
                child: CircularProgressIndicator(color: AppColors.primary),
              ),
            },
          ),
        ],
      ),
    );
  }

  Widget _chart(List<UserIndicatorModel> points) {
    final values = [for (final point in points) valueOf(point)];
    return Column(
      children: [
        Expanded(
          child: CustomPaint(
            size: Size.infinite,
            painter: IndicatorChartPainter(
              values: values,
              asLine: asLine,
              color: AppColors.primary,
              mutedColor: AppColors.progressTrack,
            ),
          ),
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            for (final point in points)
              Expanded(
                child: Text(
                  indicatorSeriesLabel(point.reference),
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 11,
                    color: AppColors.textMuted,
                  ),
                ),
              ),
          ],
        ),
      ],
    );
  }
}
