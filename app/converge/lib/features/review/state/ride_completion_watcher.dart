// Autor: Sara Fernandes Monteiro - RA 25024107

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/ride_model.dart';
import '../../ride/data/models/ride_status.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import '../../ride/state/ride_providers.dart';

/// De quanto em quanto tempo o app consulta as viagens enquanto há uma a caminho.
final rideCompletionPollIntervalProvider = Provider<Duration>(
  (ref) => const Duration(seconds: 15),
);

/// Acompanha, em silêncio, as viagens do usuário enquanto ele tem uma a caminho, para o app
/// perceber em qualquer tela quando ela é concluída (por qualquer botão do motorista) e abrir
/// a avaliação. A consulta não mexe em nenhuma tela: a lista do app só é recarregada quando
/// algum status de viagem realmente mudou (recarregar sem mudança faria as telas piscarem
/// o "carregando" a cada consulta). Sem viagem a caminho, não consulta nada.
/// Só existe enquanto alguém o observa: a Home observa.
final rideCompletionWatcherProvider = Provider.autoDispose<void>((ref) {
  final rides = ref.watch(myRidesProvider).value ?? const <RideModel>[];
  final now = DateTime.now();
  if (!rides.any((ride) => isRideOnItsWay(ride, now))) return;

  final timer = Timer.periodic(ref.read(rideCompletionPollIntervalProvider), (
    _,
  ) async {
    try {
      final fresh = await ref.read(rideRepositoryProvider).listMyRides();
      if (ref.mounted && rideStatusesChanged(rides, fresh)) {
        ref.invalidate(myRidesProvider);
      }
    } catch (_) {
      // Sem conexão agora: tenta de novo na próxima consulta.
    }
  });
  ref.onDispose(timer.cancel);
});

/// Alguma viagem mudou de status, ou apareceu ou sumiu, entre as duas listas.
bool rideStatusesChanged(List<RideModel> before, List<RideModel> after) {
  // Inclui o status e o pagamento de cada participante: na chegada de um passageiro, a
  // viagem continua em andamento e só o status dele muda (é aí que a cobrança dele é
  // emitida); depois, o "Já paguei" dele só muda o pagamento
  String signature(List<RideModel> rides) => ([
    for (final ride in rides)
      '${ride.id}:${ride.status.name}:'
          '${[for (final participant in ride.participants) '${participant.status.name}:${participant.paymentStatus?.name}'].join(',')}',
  ]..sort()).join('|');

  return signature(before) != signature(after);
}

/// Viagem que pode ser concluída em breve: em andamento, ou confirmada para sair nas
/// próximas 24 horas (ou com a saída já passada). O motorista pode iniciar antes do
/// horário, então uma carona de amanhã cedo também precisa ser acompanhada hoje.
bool isRideOnItsWay(RideModel ride, DateTime now) {
  if (ride.status == RideStatus.inProgress) return true;
  if (ride.status != RideStatus.confirmed) return false;

  final departure = ride.departureDateTime;
  return departure != null &&
      departure.isBefore(now.add(const Duration(hours: 24)));
}
