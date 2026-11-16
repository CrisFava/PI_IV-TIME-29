// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/home/presentation/widgets/ride_flow/ride_schedule_step_sheet.dart';
import 'package:converge/features/route/data/models/route_estimate_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Widget sheet({required bool isDriver}) => MaterialApp(
    home: Scaffold(
      body: RideScheduleStepSheet(
        isDriver: isDriver,
        originAddress: 'Cambuí',
        destinationAddress: 'PUC-Campinas',
        estimate: const RouteEstimateModel(
          distanceKm: 12.4,
          durationMinutes: 25,
        ),
        isEstimating: false,
        departureLabel: 'Agora',
        onChangeDeparture: () {},
        onRepeatWeeklyChanged: (_) {},
        onSearchRadiusChanged: (_) {},
        isSubmitting: false,
        onSubmit: () {},
        onEditOrigin: () {},
        onEditDestination: () {},
        onBack: () {},
        onClose: () {},
      ),
    ),
  );

  testWidgets(
    'mostra o tempo do Google e só o motorista escolhe repetir e o raio',
    (tester) async {
      tester.view.physicalSize = const Size(800, 2400);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(sheet(isDriver: true));
      expect(find.text('~25 min de viagem · 12,4 km'), findsOneWidget);
      expect(find.byType(Slider), findsOneWidget);
      expect(find.text('Revisar carona'), findsOneWidget);

      await tester.pumpWidget(sheet(isDriver: false));
      expect(find.byType(Slider), findsNothing);
      expect(find.byType(Checkbox), findsNothing);
      expect(find.text('Revisar pedido'), findsOneWidget);
    },
  );
}
