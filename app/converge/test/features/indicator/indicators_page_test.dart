// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/indicator/data/models/indicator_period.dart';
import 'package:converge/features/indicator/data/models/methodology_item.dart';
import 'package:converge/features/indicator/data/models/platform_indicator_model.dart';
import 'package:converge/features/indicator/data/models/user_indicator_model.dart';
import 'package:converge/features/indicator/data/repository/indicator_repository.dart';
import 'package:converge/features/indicator/data/repository/indicator_repository_impl.dart';
import 'package:converge/features/indicator/presentation/pages/indicators_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeIndicatorRepository implements IndicatorRepository {
  final List<IndicatorPeriod> requested = [];

  @override
  Future<UserIndicatorModel> myIndicators(IndicatorPeriod period) async {
    requested.add(period);
    return const UserIndicatorModel(
      totalRides: 128,
      ridesAsDriver: 80,
      ridesAsPassenger: 48,
      sharedKm: 1240,
      carsAvoided: 34,
      co2AvoidedKg: 186,
      equivalentTrees: 9,
    );
  }

  @override
  Future<PlatformIndicatorModel> platformIndicators(
    IndicatorPeriod period,
  ) async => const PlatformIndicatorModel(
    totalRides: 5400,
    activeUsers: 214,
    totalCo2AvoidedKg: 0,
  );

  @override
  Future<List<UserIndicatorModel>> mySeries(
    IndicatorPeriod period, {
    int limit = 6,
  }) async => [
    UserIndicatorModel(
      reference: period == IndicatorPeriod.weekly ? '2026-W46' : '2026-10',
      totalRides: 3,
      ridesAsDriver: 2,
      ridesAsPassenger: 1,
      sharedKm: 30,
      carsAvoided: 1,
      co2AvoidedKg: 4,
      equivalentTrees: 0.2,
    ),
  ];

  @override
  Future<List<MethodologyItem>> methodology() async => const [];
}

/// Testes unitários e de widgets para indicators page.
/// Cobre cenários como: mobilidade e ambiental mostram os números do período escolhido.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  testWidgets(
    'mobilidade e ambiental mostram os números do período escolhido',
    (tester) async {
      // Tela alta: os cartões e os gráficos cabem sem rolar
      tester.view.physicalSize = const Size(800, 2400);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final repository = _FakeIndicatorRepository();
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            indicatorRepositoryProvider.overrideWithValue(repository),
          ],
          child: const MaterialApp(home: IndicatorsPage()),
        ),
      );
      await tester.pumpAndSettle();

      // Mobilidade: caronas com o papel, km, carros evitados e o total da plataforma
      expect(find.text('128'), findsOneWidget);
      expect(
        find.text('80 como motorista, 48 como passageiro'),
        findsOneWidget,
      );
      expect(find.text('1.240 km'), findsOneWidget);
      expect(find.text('34'), findsOneWidget);
      expect(find.text('5.400 caronas • 214 pessoas'), findsOneWidget);
      // O gráfico segue o filtro: no mês, mês a mês; na semana, semana a semana
      expect(find.text('Viagens por mês'), findsOneWidget);
      expect(find.text('out'), findsOneWidget);
      await tester.tap(find.byKey(const ValueKey('period_WEEKLY')));
      await tester.pumpAndSettle();
      expect(find.text('Viagens por semana'), findsOneWidget);
      expect(find.text('09/11'), findsOneWidget);

      // Ambiental: CO₂ em destaque e as árvores
      await tester.tap(find.byKey(const ValueKey('indicators_tab_Ambiental')));
      await tester.pumpAndSettle();
      expect(find.text('186 kg'), findsOneWidget);
      expect(find.text('9 árvores em um ano'), findsOneWidget);
      expect(find.text('CO₂ evitado por semana (kg)'), findsOneWidget);

      // Trocar o período busca de novo
      await tester.tap(find.byKey(const ValueKey('period_ALL_TIME')));
      await tester.pumpAndSettle();
      expect(repository.requested, [
        IndicatorPeriod.monthly,
        IndicatorPeriod.weekly,
        IndicatorPeriod.allTime,
      ]);
    },
  );
}
