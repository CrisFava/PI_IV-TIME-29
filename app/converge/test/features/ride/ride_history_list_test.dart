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
  final List<Map<String, dynamic>> items;

  _FakeHistoryRepository(this.items) : super(Dio());

  @override
  Future<List<RideHistoryItem>> list(
    RideHistoryFilter filter,
    DateTime today,
  ) async => [for (final item in items) RideHistoryItem.fromJson(item)];
}

Map<String, dynamic> _place(String address, String? neighborhood) => {
  'address': address,
  'neighborhood': ?neighborhood,
  'city': 'Campinas',
  'latitude': 0,
  'longitude': 0,
};

/// Uma viagem do histórico. Sem informar nada, é a viagem concluída da passageira
/// com o horário marcado e nenhum horário real.
Map<String, dynamic> _ride({
  String role = 'PASSENGER',
  String status = 'COMPLETED',
  String? startedAt,
  String? completedAt,
  Map<String, dynamic>? origin,
  Map<String, dynamic>? destination,
  double? estimated,
  double? paid,
  String? payment,
}) => {
  'rideId': 'ride-1',
  'role': role,
  'rideStatus': status,
  'departureDateTime': '2026-11-12T07:30:00',
  'startedAt': startedAt,
  'completedAt': completedAt,
  'origin': origin ?? _place('Rua Coronel Quirino, 900', 'Cambuí'),
  'destination': destination ?? _place('PUC-Campinas', 'Campus'),
  'vehicle': {
    'model': 'Gol',
    'licensePlate': 'ABC1D23',
    'type': 'HATCH',
    'color': 'Prata',
  },
  'people': 4,
  'estimatedAmount': estimated,
  'paidAmount': paid,
  'paymentStatus': payment,
};

Future<void> _pumpHistory(
  WidgetTester tester,
  List<Map<String, dynamic>> items,
) async {
  tester.view.physicalSize = const Size(800, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        rideHistoryRepositoryProvider.overrideWithValue(
          _FakeHistoryRepository(items),
        ),
      ],
      child: const MaterialApp(home: Scaffold(body: RideHistoryList())),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets(
    'mostra a viagem com data, trajeto, carro, pessoas e o valor pago',
    (tester) async {
      await _pumpHistory(tester, [
        _ride(estimated: 6.0, paid: 6.0, payment: 'PAID'),
      ]);

      expect(find.text('12/11/2026 · 07:30'), findsOneWidget);
      expect(find.text('Cambuí → Campus'), findsOneWidget);
      expect(find.text('Gol · 4 pess. · pagou R\$ 6,00'), findsOneWidget);
      expect(find.text('Passageiro'), findsWidgets);
    },
  );

  testWidgets('lugar sem bairro mostra o começo do endereço', (tester) async {
    await _pumpHistory(tester, [
      _ride(
        origin: _place('Rua Coronel Quirino, 900', null),
        destination: _place('PUC-Campinas - Portaria Principal', null),
      ),
    ]);

    expect(
      find.text('Rua Coronel Quirino → PUC-Campinas - Portaria Principal'),
      findsOneWidget,
    );
  });

  group('horário', () {
    testWidgets('com início e fim reais, mostra a faixa', (tester) async {
      await _pumpHistory(tester, [
        _ride(
          startedAt: '2026-11-12T07:40:00',
          completedAt: '2026-11-12T08:15:00',
        ),
      ]);

      expect(find.text('12/11/2026 · 07:40 – 08:15'), findsOneWidget);
    });

    testWidgets('fim antes do horário marcado não vira uma faixa ao contrário', (
      tester,
    ) async {
      // Sem início real, o horário marcado (07:30) viria depois do fim (07:20)
      await _pumpHistory(tester, [_ride(completedAt: '2026-11-12T07:20:00')]);

      expect(find.text('12/11/2026 · 07:20'), findsOneWidget);
      expect(find.textContaining(' – '), findsNothing);
    });
  });

  testWidgets('o valor sempre vem depois de uma palavra que diz o que ele é', (
    tester,
  ) async {
    await _pumpHistory(tester, [
      _ride(estimated: 12.51, paid: 12.51, payment: 'PAID'),
      _ride(role: 'DRIVER', estimated: 8.58, paid: 1.0, payment: 'PAID'),
      _ride(estimated: 5.51, payment: 'PENDING'),
      _ride(role: 'DRIVER', estimated: 8.58),
      _ride(status: 'CANCELLED', estimated: 5.0, payment: 'NOT_APPLICABLE'),
      _ride(),
    ]);

    expect(find.text('Gol · 4 pess. · pagou R\$ 12,51'), findsOneWidget);
    expect(find.text('Gol · 4 pess. · recebeu R\$ 1,00'), findsOneWidget);
    expect(find.text('Gol · 4 pess. · a pagar R\$ 5,51'), findsOneWidget);
    expect(find.text('Gol · 4 pess. · custo R\$ 8,58'), findsOneWidget);
    expect(find.text('Gol · 4 pess. · estimado R\$ 5,00'), findsOneWidget);
    expect(find.text('Gol · 4 pess. · sem valor'), findsOneWidget);
  });
}
