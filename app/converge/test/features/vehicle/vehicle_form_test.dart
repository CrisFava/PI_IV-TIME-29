// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/vehicle/data/models/create_vehicle_request.dart';
import 'package:converge/features/vehicle/presentation/widgets/vehicle_form.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/valid_vehicle_request.dart';

void main() {
  Future<List<CreateVehicleRequest>> pumpForm(
    WidgetTester tester, {
    CreateVehicleRequest? initial,
  }) async {
    final submitted = <CreateVehicleRequest>[];
    tester.view.physicalSize = const Size(1200, 3000);
    tester.view.devicePixelRatio = 1.5;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: VehicleForm(
              initial: initial,
              submitLabel: 'Salvar carro',
              onSubmit: submitted.add,
            ),
          ),
        ),
      ),
    );
    return submitted;
  }

  Future<void> submit(WidgetTester tester) async {
    await tester.tap(find.byKey(const ValueKey('vehicle_form_submit')));
    await tester.pump();
  }

  testWidgets('formulário vazio mostra os erros e não envia', (tester) async {
    final submitted = await pumpForm(tester);

    await submit(tester);

    expect(find.text('Informe o modelo do veículo.'), findsOneWidget);
    expect(find.text('Informe a placa do veículo.'), findsOneWidget);
    expect(find.text('Informe o RENAVAM do veículo.'), findsOneWidget);
    expect(submitted, isEmpty);
  });

  testWidgets('RENAVAM inválido impede o envio', (tester) async {
    final submitted = await pumpForm(
      tester,
      initial: validVehicleRequest(renavam: '14283256657'),
    );

    await submit(tester);

    expect(find.text('RENAVAM inválido.'), findsOneWidget);
    expect(submitted, isEmpty);
  });

  testWidgets('dados válidos são enviados', (tester) async {
    final submitted = await pumpForm(tester, initial: validVehicleRequest());

    await submit(tester);

    expect(submitted, hasLength(1));
    expect(submitted.single.renavam, validRenavam);
    expect(submitted.single.fuelSource.averageConsumption, 12);
  });
}
