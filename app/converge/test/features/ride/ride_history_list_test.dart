// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/ride/data/models/ride_history_filter.dart';
import 'package:converge/features/ride/data/models/ride_history_item.dart';
import 'package:converge/features/ride/data/repository/ride_history_repository.dart';
import 'package:converge/features/ride/presentation/widgets/ride_history_list.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeHistoryRepository extends RideHistoryRepository {
  _FakeHistoryRepository() : super(Dio());

  @override
  Future<List<RideHistoryItem>> list(
    RideHistoryFilter filter,
    DateTime today,
  ) async => [
    RideHistoryItem.fromJson({
      'rideId': 'ride-1',
      'role': 'PASSENGER',
      'rideStatus': 'COMPLETED',
      'departureDateTime': '2026-11-12T07:30:00',
      'origin': {
        'address': 'Rua Coronel Quirino, 900',
        'neighborhood': 'Cambuí',
        'city': 'Campinas',
        'latitude': 0,
        'longitude': 0,
      },
      'destination': {
        'address': 'PUC-Campinas',
        'neighborhood': 'Campus',
        'city': 'Campinas',
        'latitude': 0,
        'longitude': 0,
      },
      'vehicle': {
        'model': 'Gol',
        'licensePlate': 'ABC1D23',
        'type': 'HATCH',
        'color': 'Prata',
      },
      'people': 4,
      'estimatedAmount': 6.0,
      'paidAmount': 6.0,
      'paymentStatus': 'PAID',
    }),
  ];
}

void main() {
  testWidgets(
    'mostra a viagem com data, trajeto, carro, pessoas e o valor pago',
    (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            rideHistoryRepositoryProvider.overrideWithValue(
              _FakeHistoryRepository(),
            ),
          ],
          child: const MaterialApp(home: Scaffold(body: RideHistoryList())),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('12/11/2026 · 07:30'), findsOneWidget);
      expect(find.text('Cambuí → Campus'), findsOneWidget);
      expect(find.text('Gol · 4 pess. · pagou R\$ 6,00'), findsOneWidget);
      expect(find.text('Passageiro'), findsWidgets);
    },
  );
}
