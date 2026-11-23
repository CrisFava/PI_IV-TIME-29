// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/data/models/driver_ride_request.dart';
import 'package:converge/features/matching/presentation/widgets/ride_request_card.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';

/// Testes unitários e de widgets para ride request card.
/// Cobre cenários como: com vaga livre, deixa aceitar, carona lotada: bloqueia o aceite.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
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

  testWidgets('com vaga livre, deixa aceitar', (tester) async {
    await pumpCard(tester, pendingRequest(availableSeats: 2));

    expect(find.text('Aceitar'), findsOneWidget);
    expect(acceptButton(tester).onPressed, isNotNull);
  });

  testWidgets('carona lotada: bloqueia o aceite', (tester) async {
    await pumpCard(tester, pendingRequest(availableSeats: 0));

    expect(find.text('Lotada'), findsOneWidget);
    expect(acceptButton(tester).onPressed, isNull);
  });
}
