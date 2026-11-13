// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/presentation/widgets/driver_ride_card.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';

void main() {
  Future<List<String>> pumpCard(WidgetTester tester, RideModel ride) async {
    final pressed = <String>[];
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: DriverRideCard(
              ride: ride,
              onStart: () => pressed.add('start'),
              onComplete: () => pressed.add('complete'),
              onOpenChat: (passengerId) => pressed.add('chat:$passengerId'),
            ),
          ),
        ),
      ),
    );
    return pressed;
  }

  testWidgets('sem confirmados, explica que precisa aceitar alguém', (
    tester,
  ) async {
    await pumpCard(
      tester,
      fakeRide(status: 'AWAITING_CONFIRMATION', passengerStatus: 'REQUESTED'),
    );

    expect(find.text('Iniciar trajeto'), findsNothing);
    expect(find.textContaining('Aceite um passageiro'), findsOneWidget);
  });

}
