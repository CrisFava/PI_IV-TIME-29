// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/vehicle/data/models/vehicle_model.dart';
import 'package:converge/features/vehicle/data/models/vehicle_type.dart';
import 'package:converge/features/vehicle/presentation/widgets/vehicle_list_tile.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Testes unitários e de widgets para vehicle list tile.
/// Cobre cenários como: tocar no cartão abre os detalhes; não há lixeira no card.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  const onix = VehicleModel(
    id: 'vehicle-1',
    model: 'Onix',
    licensePlate: 'ABC1D23',
    color: 'Prata',
    type: VehicleType.hatch,
    totalCapacity: 5,
  );

  testWidgets('tocar no cartão abre os detalhes; não há lixeira no card', (
    tester,
  ) async {
    var opened = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: VehicleListTile(vehicle: onix, onOpenDetails: () => opened++),
        ),
      ),
    );

    expect(find.byIcon(Icons.delete_outline_rounded), findsNothing);
    expect(find.text('Mais informações'), findsOneWidget);

    await tester.tap(find.text('Onix'));

    expect(opened, 1);
  });
}
