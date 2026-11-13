// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/data/models/driver_ride_request.dart';
import 'package:converge/features/matching/presentation/widgets/ride_request_card.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';

void main() {
  DriverRideRequest pendingRequest({required int availableSeats}) {
    final json = fakeRideJson(
      status: 'AWAITING_CONFIRMATION',
      passengerStatus: 'REQUESTED',
    )..['availableSeats'] = availableSeats;
    final ride = RideModel.fromJson(json);
    return DriverRideRequest(
      ride: ride,
      passenger: ride.participants.firstWhere((p) => p.isPassenger),
    );
  }

  Future<void> pumpCard(WidgetTester tester, DriverRideRequest request) {
    return tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: RideRequestCard(
            request: request,
            onAccept: () {},
            onReject: () {},
            onOpenChat: () {},
          ),
        ),
      ),
    );
  }

  ElevatedButton acceptButton(WidgetTester tester) =>
      tester.widget<ElevatedButton>(find.byType(ElevatedButton));

  testWidgets('mostra as vagas livres sobre o total e deixa aceitar', (
    tester,
  ) async {
    await pumpCard(tester, pendingRequest(availableSeats: 2));

    expect(find.text('2/3 vagas livres'), findsOneWidget);
    expect(find.text('Aceitar'), findsOneWidget);
    expect(acceptButton(tester).onPressed, isNotNull);
  });

  testWidgets('carona lotada: avisa e bloqueia o aceite', (tester) async {
    await pumpCard(tester, pendingRequest(availableSeats: 0));

    expect(find.text('Carona lotada (3/3)'), findsOneWidget);
    expect(find.text('Lotada'), findsOneWidget);
    expect(acceptButton(tester).onPressed, isNull);
  });
}
