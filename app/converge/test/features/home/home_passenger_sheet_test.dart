// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/home/presentation/widgets/home_passenger_sheet.dart';
import 'package:converge/features/home/presentation/widgets/passenger_ride_step.dart';
import 'package:converge/features/matching/state/passenger_match_controller.dart';
import 'package:converge/features/matching/state/passenger_match_phase.dart';
import 'package:converge/features/matching/state/passenger_match_state.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../matching/fakes/fake_matches.dart';

/// Controller que já nasce no estado pedido, sem buscar nada.
class _FixedMatchController extends PassengerMatchController {
  final PassengerMatchState _initial;

  _FixedMatchController(this._initial);

  @override
  PassengerMatchState build() => _initial;
}

const _keepSearching = 'Continuar procurando depois';

class _Taps {
  int keepSearching = 0;
  int cancelFlow = 0;
}

Future<_Taps> _pumpSheet(
  WidgetTester tester,
  PassengerMatchState state,
) async {
  tester.view.physicalSize = const Size(800, 1600);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final taps = _Taps();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        passengerMatchProvider.overrideWith(() => _FixedMatchController(state)),
      ],
      child: MaterialApp(
        home: Scaffold(
          body: Stack(
            children: [
              Positioned.fill(
                child: HomePassengerSheet(
                  step: PassengerRideStep.driverList,
                  onSelectDriver: (_) {},
                  onRetryMatches: () {},
                  onCancelRequest: () {},
                  onCancelFlow: () => taps.cancelFlow++,
                  onKeepSearching: () => taps.keepSearching++,
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
  await tester.pump();
  return taps;
}

void main() {
  group('sem motoristas compatíveis', () {
    const empty = PassengerMatchState(phase: PassengerMatchPhase.ready);

    testWidgets('explica as duas saídas e oferece continuar procurando depois', (
      tester,
    ) async {
      await _pumpSheet(tester, empty);

      expect(
        find.text(
          'Nenhum motorista compatível com o seu trajeto agora. '
          'Você pode continuar procurando depois, e o seu pedido fica ativo. '
          'Para desistir, toque no X.',
        ),
        findsOneWidget,
      );
      expect(find.text(_keepSearching), findsOneWidget);
      // O aviso antigo prometia um botão que não existia
      expect(find.textContaining('atualizar mais tarde'), findsNothing);
    });

    testWidgets('"Continuar procurando depois" não cancela o pedido', (
      tester,
    ) async {
      final taps = await _pumpSheet(tester, empty);

      await tester.tap(find.text(_keepSearching));

      expect(taps.keepSearching, 1);
      expect(taps.cancelFlow, 0);
    });

    testWidgets('o X diz que cancela a busca e cancela', (tester) async {
      final taps = await _pumpSheet(tester, empty);

      expect(find.byTooltip('Fechar'), findsNothing);
      await tester.tap(find.byTooltip('Cancelar busca'));

      expect(taps.cancelFlow, 1);
      expect(taps.keepSearching, 0);
    });
  });

  group('o botão só aparece quando não há motoristas', () {
    testWidgets('com motoristas na lista', (tester) async {
      await _pumpSheet(
        tester,
        PassengerMatchState(
          phase: PassengerMatchPhase.ready,
          matches: [fakeMatch()],
        ),
      );

      expect(find.text('Lucas Silveira • ★ 4.8'), findsOneWidget);
      expect(find.text(_keepSearching), findsNothing);
    });

    testWidgets('enquanto busca', (tester) async {
      await _pumpSheet(
        tester,
        const PassengerMatchState(phase: PassengerMatchPhase.loading),
      );

      expect(find.text(_keepSearching), findsNothing);
    });

    testWidgets('quando a busca falha', (tester) async {
      await _pumpSheet(
        tester,
        const PassengerMatchState(
          phase: PassengerMatchPhase.failed,
          errorMessage: 'Sem conexão.',
        ),
      );

      expect(find.text('Sem conexão.'), findsOneWidget);
      expect(find.text(_keepSearching), findsNothing);
    });
  });
}
