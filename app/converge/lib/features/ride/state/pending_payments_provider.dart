// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/ride_payment_model.dart';
import '../data/repository/ride_repository_impl.dart';
import 'ride_providers.dart';

/// Cobranças em aberto do usuário logado, com o PIX de cada uma.
///
/// Acompanha a lista de viagens: quando ela muda (uma viagem é concluída ou a chegada de
/// um passageiro é confirmada), as cobranças são consultadas de novo.
final pendingPaymentsProvider =
    FutureProvider.autoDispose<List<RidePaymentModel>>((ref) {
      ref.watch(myRidesProvider.select((rides) => rides.value));
      return ref.watch(rideRepositoryProvider).listPendingPayments();
    });
