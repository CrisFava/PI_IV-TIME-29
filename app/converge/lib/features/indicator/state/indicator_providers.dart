// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/indicator_period.dart';
import '../data/models/methodology_item.dart';
import '../data/models/platform_indicator_model.dart';
import '../data/models/user_indicator_model.dart';
import '../data/repository/indicator_repository_impl.dart';

/// Impacto do usuário logado no período (RF51 a RF58).
final myIndicatorsProvider = FutureProvider.autoDispose
    .family<UserIndicatorModel, IndicatorPeriod>(
      (ref, period) =>
          ref.watch(indicatorRepositoryProvider).myIndicators(period),
    );

/// Últimas semanas ou meses do usuário, para os gráficos (RF58).
final myIndicatorSeriesProvider = FutureProvider.autoDispose
    .family<List<UserIndicatorModel>, IndicatorPeriod>(
      (ref, period) => ref.watch(indicatorRepositoryProvider).mySeries(period),
    );

/// Impacto de toda a plataforma no período (RF60).
final platformIndicatorsProvider = FutureProvider.autoDispose
    .family<PlatformIndicatorModel, IndicatorPeriod>(
      (ref, period) =>
          ref.watch(indicatorRepositoryProvider).platformIndicators(period),
    );

/// Como os números são calculados (RF61).
final methodologyProvider = FutureProvider.autoDispose<List<MethodologyItem>>(
  (ref) => ref.watch(indicatorRepositoryProvider).methodology(),
);
