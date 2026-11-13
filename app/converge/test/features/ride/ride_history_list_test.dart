// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/auth/data/models/user_model.dart';
import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/ride/presentation/widgets/ride_history_list.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../auth/fakes/fake_user.dart';
import '../matching/fakes/fake_seat_ride_repository.dart';
import '../vehicle/fakes/fake_signed_in_auth_controller.dart';
import 'fakes/fake_ride.dart';

void main() {
  // O usuário logado é o motorista das viagens de fakeRide()
  UserModel signedInAs(String id) {
    final base = fakeUser();
    return UserModel(
      id: id,
      firebaseUid: base.firebaseUid,
      name: base.name,
      email: base.email,
      phone: base.phone,
      photoUrl: null,
      participationRoles: base.participationRoles,
      academicRecord: base.academicRecord,
      active: true,
    );
  }

  Future<void> pumpHistory(WidgetTester tester, List<RideModel> rides) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authControllerProvider.overrideWith(
            () => FakeSignedInAuthController(signedInAs(fakeDriverId)),
          ),
          rideRepositoryProvider.overrideWithValue(
            FakeSeatRideRepository(myRides: rides),
          ),
        ],
        child: const MaterialApp(home: Scaffold(body: RideHistoryList())),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('marca cada viagem com Motorista ou Passageiro', (tester) async {
    final asPassenger = RideModel.fromJson(
      fakeRideJson()
        ..['id'] = 'ride-2'
        ..['driverId'] = 'outro-motorista',
    );

    await pumpHistory(tester, [fakeRide(), asPassenger]);

    expect(find.text('Motorista'), findsOneWidget);
    expect(find.text('Passageiro'), findsOneWidget);
    expect(find.byType(ElevatedButton), findsNothing);
    expect(find.byType(FilledButton), findsNothing);
  });

  testWidgets('sem viagens, mostra o aviso de histórico vazio', (tester) async {
    await pumpHistory(tester, const []);

    expect(find.textContaining('Você ainda não tem viagens'), findsOneWidget);
  });
}
