// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/vehicle/data/models/vehicle_model.dart';
import 'package:converge/features/vehicle/data/models/vehicle_type.dart';
import 'package:converge/features/vehicle/presentation/widgets/vehicle_list_tile.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const onix = VehicleModel(
    id: 'vehicle-1',
    model: 'Onix',
    licensePlate: 'ABC1D23',
    color: 'Prata',
    type: VehicleType.hatch,
    totalCapacity: 5,
  );

  testWidgets('tocar no cartão abre a edição; a lixeira apaga', (tester) async {
    final taps = <String>[];
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: VehicleListTile(
            vehicle: onix,
            onEdit: () => taps.add('editar'),
            onDelete: () => taps.add('apagar'),
          ),
        ),
      ),
    );

    expect(find.byIcon(Icons.edit_rounded), findsNothing);

    await tester.tap(find.text('Onix'));
    await tester.tap(find.byIcon(Icons.delete_outline_rounded));

    expect(taps, ['editar', 'apagar']);
  });
}
