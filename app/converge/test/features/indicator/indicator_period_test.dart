// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/indicator/data/models/indicator_period.dart';
import 'package:flutter_test/flutter_test.dart';

/// Testes unitários e de widgets para indicator period.
/// Cobre cenários como: a referência é a do período atual, no formato do backend.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  test('a referência é a do período atual, no formato do backend', () {
    final date = DateTime(2026, 11, 16);

    expect(IndicatorPeriod.monthly.referenceFor(date), '2026-11');
    expect(IndicatorPeriod.semesterly.referenceFor(date), '2026-S2');
    expect(IndicatorPeriod.weekly.referenceFor(date), '2026-W47');
    // Semana ISO na virada do ano: 1/1/2027 (sexta) ainda é a semana 53 de 2026
    expect(
      IndicatorPeriod.weekly.referenceFor(DateTime(2027, 1, 1)),
      '2026-W53',
    );
    expect(IndicatorPeriod.allTime.referenceFor(date), isNull);
  });
}
