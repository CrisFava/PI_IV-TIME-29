// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/core/widgets/ride_dock_navigation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Future<void> pumpDock(
    WidgetTester tester, {
    required bool isOnShift,
    VoidCallback? onStartDriverShift,
    VoidCallback? onEndDriverShift,
    bool needsVehicle = false,
    VoidCallback? onAddVehicle,
    bool isSearchingRide = false,
    VoidCallback? onOpenRideSearch,
    VoidCallback? onCancelRideSearch,
  }) {
    return tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Align(
            alignment: Alignment.bottomCenter,
            child: RideDockNavigation(
              currentIndex: 0,
              onTabSelected: (_) {},
              onRequestRide: () {},
              onStartDriverShift: onStartDriverShift ?? () {},
              isOnShift: isOnShift,
              onEndDriverShift: onEndDriverShift,
              needsVehicle: needsVehicle,
              onAddVehicle: onAddVehicle,
              isSearchingRide: isSearchingRide,
              rideSearchDestination: 'PUC-Campinas, Campus I',
              onOpenRideSearch: onOpenRideSearch,
              onCancelRideSearch: onCancelRideSearch,
            ),
          ),
        ),
      ),
    );
  }

  Future<void> openMenu(WidgetTester tester) async {
    // O botão do carro é o último ícone de carro da árvore (o do cabeçalho do menu vem antes)
    await tester.tap(find.byIcon(Icons.directions_car_rounded).last);
    // A animação começa no próximo frame; o anel verde repete para sempre,
    // então avança o tempo em vez de esperar tudo parar
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
  }

  testWidgets('fora do expediente, o menu oferece iniciar expediente', (
    tester,
  ) async {
    await pumpDock(tester, isOnShift: false);
    await openMenu(tester);

    expect(find.bySemanticsLabel('Fechar opções de Caronas'), findsOneWidget);
    expect(find.text('Iniciar expediente de motorista'), findsOneWidget);
    expect(find.text('Em expediente'), findsNothing);
    expect(find.text('Encerrar'), findsNothing);
  });

  testWidgets('em expediente, o menu mostra o estado e encerra', (
    tester,
  ) async {
    var ended = false;
    var opened = false;
    await pumpDock(
      tester,
      isOnShift: true,
      onStartDriverShift: () => opened = true,
      onEndDriverShift: () => ended = true,
    );
    await openMenu(tester);

    expect(find.bySemanticsLabel('Fechar opções de Caronas'), findsOneWidget);
    expect(find.text('Em expediente'), findsOneWidget);
    expect(find.text('Iniciar expediente de motorista'), findsNothing);

    await tester.tap(find.text('Em expediente'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    expect(opened, isTrue);

    await openMenu(tester);
    await tester.tap(find.byKey(const ValueKey('end_driver_shift_button')));
    await tester.pump(const Duration(milliseconds: 500));
    expect(ended, isTrue);
  });

  testWidgets('motorista sem carro: bloqueado com o botão Adicionar carro', (
    tester,
  ) async {
    var addVehicle = false;
    await pumpDock(
      tester,
      isOnShift: false,
      needsVehicle: true,
      onAddVehicle: () => addVehicle = true,
    );
    await openMenu(tester);

    expect(find.text('Bloqueado'), findsOneWidget);
    expect(
      find.text('Adicione um carro para ter acesso a essa função.'),
      findsOneWidget,
    );
    expect(find.text('Iniciar expediente de motorista'), findsNothing);

    await tester.tap(
      find.byKey(const ValueKey('add_vehicle_from_dock_button')),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    expect(addVehicle, isTrue);
  });

  testWidgets(
    'procurando motorista: mostra o destino, abre os detalhes e cancela',
    (tester) async {
      var opened = false;
      var cancelled = false;
      await pumpDock(
        tester,
        isOnShift: false,
        isSearchingRide: true,
        onOpenRideSearch: () => opened = true,
        onCancelRideSearch: () => cancelled = true,
      );
      await openMenu(tester);

      expect(find.text('Procurando motorista'), findsOneWidget);
      expect(find.text('Para: PUC-Campinas, Campus I'), findsOneWidget);
      expect(find.text('Pedir carona'), findsNothing);

      await tester.tap(find.text('Procurando motorista'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));
      expect(opened, isTrue);

      await openMenu(tester);
      await tester.tap(find.byKey(const ValueKey('cancel_ride_search_button')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));
      expect(cancelled, isTrue);
    },
  );
}
