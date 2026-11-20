// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/participant_status.dart';
import '../../ride/data/models/ride_status.dart';
import '../../ride/state/ride_providers.dart';
import '../data/models/driver_ride_request.dart';
import 'matching_viewer_provider.dart';

const _openRideStatuses = {
  RideStatus.awaitingConfirmation,
  RideStatus.confirmed,
  RideStatus.inProgress,
};

/// Pedidos de vaga que o motorista ainda pode aceitar: só os das viagens com
/// vaga (com o carro cheio eles somem e só voltam se alguém sair), do mais
/// antigo ao mais novo. Com [query], só os passageiros cujo nome contém o texto.
List<DriverRideRequest> pendingRideRequests(
  List<DriverRideRequest> requests, {
  String query = '',
}) {
  final search = _normalized(query.trim());
  final pending = [
    for (final request in requests)
      if (request.passenger.status == ParticipantStatus.requested &&
          request.ride.availableSeats > 0 &&
          (search.isEmpty ||
              _normalized(request.passenger.displayName).contains(search)))
        request,
  ];
  // Sem horário do pedido, vai para o fim
  final far = DateTime(9999);
  pending.sort(
    (a, b) => (a.passenger.requestedAt ?? far).compareTo(
      b.passenger.requestedAt ?? far,
    ),
  );
  return pending;
}

/// Minúsculas e sem acento, para "joao" achar "João".
String _normalized(String text) {
  const from = 'áàâãäéèêëíìîïóòôõöúùûüç';
  const to = 'aaaaaeeeeiiiiooooouuuuc';
  final lower = text.toLowerCase();
  final buffer = StringBuffer();
  for (final char in lower.split('')) {
    final index = from.indexOf(char);
    buffer.write(index < 0 ? char : to[index]);
  }
  return buffer.toString();
}

/// Passageiros nas viagens em aberto que o usuário logado dirige: os pedidos
/// esperando resposta e os já aceitos. Recarregue com `ref.invalidate(myRidesProvider)`.
final driverRideRequestsProvider =
    FutureProvider.autoDispose<List<DriverRideRequest>>((ref) async {
      final viewerId = ref.watch(matchingViewerIdProvider);
      if (viewerId == null) return const [];

      final rides = await ref.watch(myRidesProvider.future);
      return [
        for (final ride in rides)
          if (ride.isDrivenBy(viewerId) &&
              _openRideStatuses.contains(ride.status))
            for (final participant in ride.participants)
              if (participant.isPassenger &&
                  participant.userId != null &&
                  (participant.status == ParticipantStatus.requested ||
                      participant.status.hasSeat))
                DriverRideRequest(ride: ride, passenger: participant),
      ];
    });
